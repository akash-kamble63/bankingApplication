package com.account_service.kafka;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.enums.BeneficiaryStatus;
import com.account_service.kafka.NotificationEventPublisher;

import com.account_service.repository.BeneficiaryRepository;
import com.account_service.strategy.SagaCompensator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeneficiaryVerificationSagaCompensator implements SagaCompensator {

    private final ObjectMapper objectMapper;
    private final BeneficiaryRepository beneficiaryRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    public String sagaType() {
        return "BENEFICIARY_VERIFICATION";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(String step, String payload, String compensationData)
            throws CompensationException {

        try {
            log.info("Compensating beneficiary verification step: {}", step);

            switch (step) {
                case "VERIFY_ACCOUNT_EXISTS" -> compensateAccountVerification();
                case "VALIDATE_IFSC" -> compensateIfscValidation();
                case "MARK_VERIFIED" -> compensateMarkVerified(payload, compensationData);
                case "SEND_CONFIRMATION" -> compensateSendConfirmation(payload, compensationData);
                default -> log.info("No compensation needed for step: {}", step);
            }

            log.info("Successfully compensated step: {}", step);

        } catch (Exception e) {
            log.error("Compensation failed for step: {}", step, e);
            throw new CompensationException(step,
                    "Failed to compensate beneficiary verification step", e, true);
        }
    }

    @Override
    public boolean canCompensate(String step) {
        return switch (step) {
            case "VERIFY_ACCOUNT_EXISTS", "VALIDATE_IFSC" -> false;
            case "MARK_VERIFIED", "SEND_CONFIRMATION" -> true;
            default -> true;
        };
    }

    private void compensateAccountVerification() {
        log.debug("No compensation needed for account verification (read-only)");
    }

    private void compensateIfscValidation() {
        log.debug("No compensation needed for IFSC validation (read-only)");
    }

    /**
     * Revert beneficiary verification status
     */
    private void compensateMarkVerified(String payload, String compensationData)
            throws Exception {

        log.info("Reverting beneficiary verification status");

        JsonNode payloadData = objectMapper.readTree(payload);
        Long beneficiaryId = payloadData.get("beneficiaryId").asLong();

        beneficiaryRepository.findById(beneficiaryId).ifPresent(beneficiary -> {
            beneficiary.setIsVerified(false);
            beneficiary.setVerifiedAt(null);
            beneficiary.setStatus(BeneficiaryStatus.PENDING_VERIFICATION);
            beneficiaryRepository.save(beneficiary);

            log.info("Reverted verification for beneficiary: {}", beneficiaryId);
        });
    }

    /**
     * Send verification failure notification via Kafka to notification-service
     */
    private void compensateSendConfirmation(String payload, String compensationData)
            throws Exception {

        log.info("Sending verification failure notification");

        JsonNode payloadData = objectMapper.readTree(payload);
        Long userId = payloadData.get("userId").asLong();
        String beneficiaryName = payloadData.get("beneficiaryName").asText();
        String accountNumber = payloadData.has("accountNumber")
                ? payloadData.get("accountNumber").asText()
                : "Unknown";

        notificationEventPublisher.publishVerificationFailedNotification(
                userId,
                beneficiaryName,
                accountNumber);

        log.info("Sent verification failure notification event to Kafka for user: {}", userId);
    }
}