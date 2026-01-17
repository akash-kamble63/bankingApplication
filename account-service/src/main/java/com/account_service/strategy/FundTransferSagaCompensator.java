package com.account_service.strategy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.kafka.NotificationEventPublisher;
import com.account_service.patterns.AccountMetrics;

import com.account_service.service.AccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundTransferSagaCompensator implements SagaCompensator {
    private final ObjectMapper objectMapper;
    private final AccountMetrics metrics;
    private final AccountService accountService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    public String sagaType() {
        return "FUND_TRANSFER";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(String step, String payload, String compensationData)
            throws CompensationException {

        Timer.Sample sample = metrics.startTimer();

        try {
            log.info("Compensating fund transfer step: {}", step);

            switch (step) {
                case "VALIDATE_BENEFICIARY" -> compensateValidation(payload, compensationData);
                case "RESERVE_FUNDS" -> compensateReserveFunds(payload, compensationData);
                case "DEBIT_SOURCE_ACCOUNT" -> compensateDebit(payload, compensationData);
                case "CREDIT_DESTINATION_ACCOUNT" -> compensateCredit(payload, compensationData);
                case "RELEASE_HOLD" -> compensateReleaseHold(payload, compensationData);
                case "SEND_NOTIFICATION" -> compensateNotification(payload, compensationData);
                default -> log.info("No compensation needed for step: {}", step);
            }

            metrics.recordEventStoreOperation(sample, "COMPENSATION_" + step, true);
            log.info("Successfully compensated step: {}", step);

        } catch (Exception e) {
            metrics.recordEventStoreOperation(sample, "COMPENSATION_" + step, false);
            log.error("Compensation failed for step: {}", step, e);

            boolean retriable = isRetriableError(e);
            throw new CompensationException(step,
                    "Failed to compensate step: " + step, e, retriable);
        }
    }

    @Override
    public boolean canCompensate(String step) {
        return switch (step) {
            case "VALIDATE_BENEFICIARY", "SEND_NOTIFICATION" -> false;
            case "RESERVE_FUNDS", "DEBIT_SOURCE_ACCOUNT",
                    "CREDIT_DESTINATION_ACCOUNT", "RELEASE_HOLD" ->
                true;
            default -> true;
        };
    }

    /**
     * Compensate validation step (no-op, validation is idempotent)
     */
    private void compensateValidation(String payload, String compensationData) {
        log.debug("No compensation needed for validation");
    }

    /**
     * Compensate fund reservation - release the hold
     */
    private void compensateReserveFunds(String payload, String compensationData)
            throws Exception {

        log.info("Compensating RESERVE_FUNDS: releasing hold");

        JsonNode compData = objectMapper.readTree(compensationData);
        Long holdId = compData.get("holdId").asLong();
        Long accountId = compData.get("accountId").asLong();

        accountService.releaseHold(accountId, holdId);

        log.info("Released hold: holdId={}, accountId={}", holdId, accountId);
    }

    /**
     * Compensate debit - credit back to source account
     */
    private void compensateDebit(String payload, String compensationData)
            throws Exception {

        log.info("Compensating DEBIT_SOURCE_ACCOUNT: crediting back to source");

        JsonNode payloadData = objectMapper.readTree(payload);
        JsonNode compData = objectMapper.readTree(compensationData);

        Long sourceAccountId = compData.get("sourceAccountId").asLong();
        String amount = payloadData.get("amount").asText();
        String transactionId = compData.get("transactionId").asText();

        accountService.credit(
                sourceAccountId,
                new BigDecimal(amount),
                "REVERSAL_" + transactionId,
                "Fund transfer reversal - compensation");

        log.info("Reversed debit: account={}, amount={}", sourceAccountId, amount);
    }

    /**
     * Compensate credit - debit from destination account
     */
    private void compensateCredit(String payload, String compensationData)
            throws Exception {

        log.info("Compensating CREDIT_DESTINATION_ACCOUNT: debiting from destination");

        JsonNode payloadData = objectMapper.readTree(payload);
        JsonNode compData = objectMapper.readTree(compensationData);

        Long destinationAccountId = compData.get("destinationAccountId").asLong();
        String amount = payloadData.get("amount").asText();
        String transactionId = compData.get("transactionId").asText();

        accountService.debit(
                destinationAccountId,
                new BigDecimal(amount),
                "REVERSAL_" + transactionId,
                "Fund transfer reversal - compensation");

        log.info("Reversed credit: account={}, amount={}", destinationAccountId, amount);
    }

    /**
     * Compensate release hold (shouldn't happen as it's the last step)
     */
    private void compensateReleaseHold(String payload, String compensationData) {
        log.debug("No compensation needed for RELEASE_HOLD (should not be called)");
    }

    /**
     * Compensate notification - send failure notification via Kafka
     */
    private void compensateNotification(String payload, String compensationData)
            throws Exception {

        log.info("Compensating SEND_NOTIFICATION: sending failure notification");

        JsonNode payloadData = objectMapper.readTree(payload);
        Long userId = payloadData.get("userId").asLong();
        String sourceAccountNumber = payloadData.get("sourceAccountNumber").asText();
        String destinationAccountNumber = payloadData.get("destinationAccountNumber").asText();
        String amount = payloadData.get("amount").asText();

        notificationEventPublisher.publishTransferFailedNotification(
                userId,
                sourceAccountNumber,
                destinationAccountNumber,
                amount);

        log.info("Sent transfer failure notification event to Kafka for user: {}", userId);
    }

    /**
     * Determine if error is retriable
     */
    private boolean isRetriableError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout")
                || message.contains("connection")
                || message.contains("unavailable")
                || message.contains("temporarily")
                || message.contains("network");
    }
}
