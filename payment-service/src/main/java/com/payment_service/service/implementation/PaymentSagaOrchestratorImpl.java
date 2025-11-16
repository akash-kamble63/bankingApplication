package com.payment_service.service.implementation;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.DTOs.BillPaymentResponse;
import com.payment_service.DTOs.BillerValidationResponse;
import com.payment_service.DTOs.FraudCheckResult;
import com.payment_service.DTOs.GatewayAuthorizationResponse;
import com.payment_service.DTOs.GatewayCaptureResponse;
import com.payment_service.DTOs.HoldResponse;
import com.payment_service.DTOs.PaymentSagaData;
import com.payment_service.DTOs.SagaResult;
import com.payment_service.DTOs.UpiStatusResponse;
import com.payment_service.DTOs.UpiTransactionResponse;
import com.payment_service.clients.AccountServiceClient;
import com.payment_service.clients.FraudServiceClient;
import com.payment_service.clients.MerchantServiceClient;
import com.payment_service.clients.NotificationServiceClient;
import com.payment_service.clients.PaymentGatewayClient;
import com.payment_service.entity.SagaState;
import com.payment_service.enums.SagaStatus;
import com.payment_service.exception.FraudException;
import com.payment_service.exception.PaymentGatewayException;
import com.payment_service.repository.SagaStateRepository;
import com.payment_service.service.PaymentSagaOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSagaOrchestratorImpl implements PaymentSagaOrchestrator{
	private final SagaStateRepository sagaStateRepository;
    private final PaymentGatewayClient gatewayClient;
    private final AccountServiceClient accountClient;
    private final FraudServiceClient fraudClient;
    private final NotificationServiceClient notificationClient;
    private final MerchantServiceClient merchantClient;
    
    /**
     * Execute card payment saga
     */
    @Transactional
    public SagaResult executeCardPaymentSaga(PaymentSagaData data) {
        String sagaId = UUID.randomUUID().toString();
        data.setSagaId(sagaId);
        
        // Create saga state
        SagaState saga = SagaState.builder()
            .sagaId(sagaId)
            .sagaType("CARD_PAYMENT")
            .status(SagaStatus.STARTED)
            .payload(toJson(data))
            .build();
        sagaStateRepository.save(saga);
        
        try {
            // Step 1: Quick fraud check (sync - 50ms)
            log.info("Saga [{}] Step 1: Quick fraud check", sagaId);
            FraudCheckResult fraudCheck = fraudClient.quickCheck(
                data.getUserId(),
                data.getAmount(),
                data.getPaymentMethod(),
                data.getPaymentReference()
            );
            
            if (fraudCheck.isBlocked()) {
                throw new FraudException("Payment blocked: " + fraudCheck.getReason());
            }
            data.setFraudScore(fraudCheck.getFraudScore());
            updateSagaStep(sagaId, "FRAUD_CHECK", SagaStatus.PROCESSING);
            
            // Step 2: Validate and reserve funds from account
            if (data.getAccountId() != null) {
                log.info("Saga [{}] Step 2: Reserve funds from account", sagaId);
                HoldResponse hold = accountClient.placeHold(
                    data.getAccountId(),
                    data.getAmount(),
                    "Card payment: " + data.getPaymentReference(),
                    data.getPaymentReference()
                );
                data.setHoldReference(hold.getHoldReference());
                data.setFundsReserved(true);
                updateSagaStep(sagaId, "RESERVE_FUNDS", SagaStatus.PROCESSING);
            }
            
            // Step 3: Authorize payment with gateway
            log.info("Saga [{}] Step 3: Authorize with gateway", sagaId);
            GatewayAuthorizationResponse authResponse = gatewayClient.authorizePayment(
                data.getGatewayName(),
                data.getCardToken(),
                data.getAmount(),
                data.getCurrency(),
                data.getPaymentReference()
            );
            
            if (!authResponse.isSuccess()) {
                throw new PaymentGatewayException(
                    "Authorization failed: " + authResponse.getErrorMessage()
                );
            }
            
            data.setGatewayPaymentId(authResponse.getGatewayPaymentId());
            data.setAuthorizationCode(authResponse.getAuthorizationCode());
            data.setPaymentAuthorized(true);
            updateSagaStep(sagaId, "AUTHORIZE_PAYMENT", SagaStatus.PROCESSING);
            
            // Step 4: Capture payment
            log.info("Saga [{}] Step 4: Capture payment", sagaId);
            GatewayCaptureResponse captureResponse = gatewayClient.capturePayment(
                data.getGatewayName(),
                authResponse.getGatewayPaymentId(),
                data.getAmount()
            );
            
            if (!captureResponse.isSuccess()) {
                throw new PaymentGatewayException(
                    "Capture failed: " + captureResponse.getErrorMessage()
                );
            }
            
            data.setExternalTransactionId(captureResponse.getTransactionId());
            data.setPaymentCaptured(true);
            updateSagaStep(sagaId, "CAPTURE_PAYMENT", SagaStatus.PROCESSING);
            
            // Step 5: Debit account (if funds were reserved)
            if (data.getAccountId() != null) {
                log.info("Saga [{}] Step 5: Debit account", sagaId);
                accountClient.debitWithIdempotency(
                    data.getAccountId(),
                    data.getAmount(),
                    data.getPaymentReference() + "-DEBIT",
                    data.getPaymentReference()
                );
                data.setAccountDebited(true);
                updateSagaStep(sagaId, "DEBIT_ACCOUNT", SagaStatus.PROCESSING);
                
                // Release hold
                accountClient.releaseHold(data.getHoldReference());
            }
            
            // Step 6: Credit merchant (if applicable)
            if (data.getMerchantId() != null) {
                log.info("Saga [{}] Step 6: Credit merchant", sagaId);
                merchantClient.creditMerchant(
                    data.getMerchantId(),
                    data.getAmount(),
                    data.getPaymentReference()
                );
                data.setMerchantCredited(true);
                updateSagaStep(sagaId, "CREDIT_MERCHANT", SagaStatus.PROCESSING);
            }
            
            // Step 7: Deep fraud analysis (async)
            CompletableFuture.runAsync(() -> 
                fraudClient.deepAnalysis(data)
            );
            
            // Step 8: Send notifications (async)
            CompletableFuture.runAsync(() -> 
                notificationClient.sendPaymentNotification(data)
            );
            
            // Complete saga
            completeSaga(sagaId);
            log.info("Saga [{}] completed successfully", sagaId);
            
            return SagaResult.success(sagaId, "Payment completed");
            
        } catch (Exception e) {
            log.error("Saga [{}] failed: {}", sagaId, e.getMessage(), e);
            compensateSaga(sagaId, data, e);
            
            // Store error details
            data.setGatewayErrorCode(extractErrorCode(e));
            data.setGatewayErrorMessage(e.getMessage());
            
            return SagaResult.failure(sagaId, e.getMessage());
        }
    }
    
    /**
     * Execute UPI payment saga
     */
    @Transactional
    public SagaResult executeUpiPaymentSaga(PaymentSagaData data) {
        String sagaId = UUID.randomUUID().toString();
        data.setSagaId(sagaId);
        
        SagaState saga = SagaState.builder()
            .sagaId(sagaId)
            .sagaType("UPI_PAYMENT")
            .status(SagaStatus.STARTED)
            .payload(toJson(data))
            .build();
        sagaStateRepository.save(saga);
        
        try {
            // Step 1: Fraud check
            FraudCheckResult fraudCheck = fraudClient.quickCheck(
                data.getUserId(),
                data.getAmount(),
                data.getPaymentMethod(),
                data.getPaymentReference()
            );
            
            if (fraudCheck.isBlocked()) {
                throw new FraudException("UPI payment blocked");
            }
            
            // Step 2: Initiate UPI transaction
            log.info("Saga [{}] Step 2: Initiate UPI transaction", sagaId);
            UpiTransactionResponse upiResponse = gatewayClient.initiateUpiTransaction(
                data.getUpiId(),
                data.getAmount(),
                data.getPaymentReference()
            );
            
            if (!upiResponse.isSuccess()) {
                throw new PaymentGatewayException("UPI initiation failed");
            }
            
            data.setUpiTransactionId(upiResponse.getTransactionId());
            data.setPaymentAuthorized(true);
            
            // Step 3: Poll for UPI status (with timeout)
            log.info("Saga [{}] Step 3: Wait for UPI confirmation", sagaId);
            boolean confirmed = waitForUpiConfirmation(
                upiResponse.getTransactionId(), 
                180 // 3 minutes timeout
            );
            
            if (!confirmed) {
                throw new PaymentGatewayException("UPI transaction timeout");
            }
            
            data.setPaymentCaptured(true);
            
            // Complete saga
            completeSaga(sagaId);
            return SagaResult.success(sagaId, "UPI payment completed");
            
        } catch (Exception e) {
            log.error("UPI saga [{}] failed: {}", sagaId, e.getMessage());
            compensateSaga(sagaId, data, e);
            return SagaResult.failure(sagaId, e.getMessage());
        }
    }
    
    /**
     * Execute bill payment saga
     */
    @Transactional
    public SagaResult executeBillPaymentSaga(PaymentSagaData data) {
        String sagaId = UUID.randomUUID().toString();
        data.setSagaId(sagaId);
        
        SagaState saga = SagaState.builder()
            .sagaId(sagaId)
            .sagaType("BILL_PAYMENT")
            .status(SagaStatus.STARTED)
            .payload(toJson(data))
            .build();
        sagaStateRepository.save(saga);
        
        try {
            // Step 1: Validate biller
            log.info("Saga [{}] Step 1: Validate biller", sagaId);
            BillerValidationResponse billerValidation = gatewayClient.validateBiller(
                data.getBillerId(),
                data.getBillNumber()
            );
            
            if (!billerValidation.isValid()) {
                throw new IllegalArgumentException("Invalid biller information");
            }
            
            // Step 2: Debit account
            if (data.getAccountId() != null) {
                accountClient.debitWithIdempotency(
                    data.getAccountId(),
                    data.getAmount(),
                    data.getPaymentReference() + "-BILL",
                    data.getPaymentReference()
                );
                data.setAccountDebited(true);
            }
            
            // Step 3: Pay bill through gateway
            log.info("Saga [{}] Step 3: Process bill payment", sagaId);
            BillPaymentResponse billResponse = gatewayClient.payBill(
                data.getBillerId(),
                data.getBillNumber(),
                data.getAmount(),
                data.getPaymentReference()
            );
            
            if (!billResponse.isSuccess()) {
                throw new PaymentGatewayException("Bill payment failed");
            }
            
            data.setExternalTransactionId(billResponse.getTransactionId());
            data.setPaymentCaptured(true);
            
            completeSaga(sagaId);
            return SagaResult.success(sagaId, "Bill payment completed");
            
        } catch (Exception e) {
            log.error("Bill payment saga [{}] failed: {}", sagaId, e.getMessage());
            compensateSaga(sagaId, data, e);
            return SagaResult.failure(sagaId, e.getMessage());
        }
    }
    
    /**
     * Compensate saga (rollback)
     */
    @Transactional
    public void compensateSaga(String sagaId, PaymentSagaData data, Exception error) {
        log.warn("Compensating payment saga: {}", sagaId);
        
        try {
            SagaState saga = sagaStateRepository.findBySagaId(sagaId)
                .orElseThrow();
            
            saga.setStatus(SagaStatus.COMPENSATING);
            saga.setErrorMessage(error.getMessage());
            sagaStateRepository.save(saga);
            
            // Compensate in reverse order
            
            if (data.isMerchantCredited()) {
                log.info("Compensating: Reversing merchant credit");
                merchantClient.reverseMerchantCredit(
                    data.getMerchantId(),
                    data.getAmount(),
                    data.getPaymentReference()
                );
            }
            
            if (data.isAccountDebited()) {
                log.info("Compensating: Reversing account debit");
                accountClient.creditWithIdempotency(
                    data.getAccountId(),
                    data.getAmount(),
                    data.getPaymentReference() + "-COMP-DEBIT",
                    data.getPaymentReference() + "-COMP"
                );
            }
            
            if (data.isPaymentCaptured()) {
                log.info("Compensating: Refunding captured payment");
                gatewayClient.refundPayment(
                    data.getGatewayName(),
                    data.getGatewayPaymentId(),
                    data.getAmount()
                );
            }
            
            if (data.isPaymentAuthorized() && !data.isPaymentCaptured()) {
                log.info("Compensating: Voiding authorization");
                gatewayClient.voidAuthorization(
                    data.getGatewayName(),
                    data.getGatewayPaymentId()
                );
            }
            
            if (data.isFundsReserved()) {
                log.info("Compensating: Releasing hold");
                accountClient.releaseHold(data.getHoldReference());
            }
            
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaStateRepository.save(saga);
            
        } catch (Exception compensationError) {
            log.error("❌ CRITICAL: Compensation failed for saga {}: {}", 
                sagaId, compensationError.getMessage());
            storeInDLQ(sagaId, data, error, compensationError);
        }
    }
    
    // Helper methods
    private void updateSagaStep(String sagaId, String step, SagaStatus status) {
        sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
            saga.setCurrentStep(step);
            saga.setStatus(status);
            sagaStateRepository.save(saga);
        });
    }
    
    private void completeSaga(String sagaId) {
        sagaStateRepository.findBySagaId(sagaId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCompletedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);
        });
    }
    
    private boolean waitForUpiConfirmation(String transactionId, int timeoutSeconds) {
        // Poll UPI gateway for status
        int attempts = timeoutSeconds / 5; // Poll every 5 seconds
        
        for (int i = 0; i < attempts; i++) {
            try {
                Thread.sleep(5000);
                UpiStatusResponse status = gatewayClient.checkUpiStatus(transactionId);
                
                if (status.isCompleted()) {
                    return true;
                }
                if (status.isFailed()) {
                    return false;
                }
                // Continue polling if pending
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false; // Timeout
    }
    
    private String extractErrorCode(Exception e) {
        if (e instanceof PaymentGatewayException) {
            return ((PaymentGatewayException) e).getErrorCode();
        }
        return "UNKNOWN";
    }
    
    private String toJson(Object obj) {
        // Use ObjectMapper
        return "{}"; // Simplified
    }
    
    private void storeInDLQ(String sagaId, PaymentSagaData data, 
                           Exception originalError, Exception compensationError) {
        log.error("Storing failed payment saga in DLQ: {}", sagaId);
        // Store in dead letter queue for manual intervention
    }
}
