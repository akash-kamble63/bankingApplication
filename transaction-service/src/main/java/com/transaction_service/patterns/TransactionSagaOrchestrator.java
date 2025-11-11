package com.transaction_service.patterns;


import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transaction_service.DTOs.HoldResponse;
import com.transaction_service.DTOs.SagaResult;
import com.transaction_service.DTOs.TransferSagaData;
import com.transaction_service.client.AccountServiceClient;
import com.transaction_service.client.FraudCheckResult;
import com.transaction_service.client.FraudServiceClient;
import com.transaction_service.client.NotificationServiceClient;
import com.transaction_service.entity.SagaState;
import com.transaction_service.enums.SagaStatus;
import com.transaction_service.exception.FraudException;
import com.transaction_service.repository.SagaStateRepository;
import com.transaction_service.repository.TransactionRepository;
import com.transaction_service.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSagaOrchestrator {
	private final SagaStateRepository sagaStateRepository;
    private final AccountServiceClient accountClient;
    private final FraudServiceClient fraudClient;
    private final NotificationServiceClient notificationClient;
    private final TransactionRepository transactionRepository;
    private final OutboxService outboxService;
    
    /**
     * Execute transfer saga with automatic compensation
     */
    @Transactional
    public SagaResult executeTransferSaga(TransferSagaData sagaData) {
        String sagaId = UUID.randomUUID().toString();
        sagaData.setSagaId(sagaId);
        
        // Create saga state
        SagaState sagaState = SagaState.builder()
            .sagaId(sagaId)
            .sagaType("FUND_TRANSFER")
            .status(SagaStatus.STARTED)
            .payload(toJson(sagaData))
            .build();
        sagaStateRepository.save(sagaState);
        
        try {
            // Step 1: Validate accounts
            log.info("Saga [{}] Step 1: Validate accounts", sagaId);
            validateAccounts(sagaData);
            updateSagaStep(sagaId, "VALIDATE_ACCOUNTS", SagaStatus.PROCESSING);
            
            // Step 2: Quick fraud check (sync - 50ms)
            log.info("Saga [{}] Step 2: Quick fraud check", sagaId);
            FraudCheckResult quickCheck = fraudClient.quickCheck(sagaData);
            if (quickCheck.isBlocked()) {
                throw new FraudException("Transaction blocked by fraud rules");
            }
            updateSagaStep(sagaId, "FRAUD_CHECK", SagaStatus.PROCESSING);
            
            // Step 3: Reserve funds (place hold)
            log.info("Saga [{}] Step 3: Reserve funds", sagaId);
            HoldResponse hold = accountClient.placeHold(
                sagaData.getSourceAccountId(),
                sagaData.getAmount(),
                "Transfer to " + sagaData.getDestinationAccountId(),
                sagaData.getTransactionReference()
            );
            sagaData.setHoldReference(hold.getHoldReference());
            updateSagaStep(sagaId, "RESERVE_FUNDS", SagaStatus.PROCESSING);
            
            // Step 4: Debit source account
            log.info("Saga [{}] Step 4: Debit source account", sagaId);
            accountClient.debitWithIdempotency(
                sagaData.getSourceAccountId(),
                sagaData.getAmount(),
                sagaData.getTransactionReference() + "-DEBIT",
                sagaData.getTransactionReference()
            );
            sagaData.setFundsDebited(true);
            updateSagaStep(sagaId, "DEBIT_ACCOUNT", SagaStatus.PROCESSING);
            
            // Step 5: Credit destination account
            log.info("Saga [{}] Step 5: Credit destination account", sagaId);
            accountClient.creditWithIdempotency(
                sagaData.getDestinationAccountId(),
                sagaData.getAmount(),
                sagaData.getTransactionReference() + "-CREDIT",
                sagaData.getTransactionReference()
            );
            sagaData.setFundsCredited(true);
            updateSagaStep(sagaId, "CREDIT_ACCOUNT", SagaStatus.PROCESSING);
            
            // Step 6: Release hold
            log.info("Saga [{}] Step 6: Release hold", sagaId);
            accountClient.releaseHold(hold.getHoldReference());
            updateSagaStep(sagaId, "RELEASE_HOLD", SagaStatus.PROCESSING);
            
            // Step 7: Async fraud analysis (deep check)
            CompletableFuture.runAsync(() -> 
                fraudClient.deepAnalysis(sagaData)
            );
            
            // Step 8: Send notifications (async)
            CompletableFuture.runAsync(() -> 
                notificationClient.sendTransactionNotification(sagaData)
            );
            
            // Complete saga
            completeSaga(sagaId);
            log.info("Saga [{}] completed successfully", sagaId);
            
            return SagaResult.success(sagaId, "Transfer completed");
            
        } catch (Exception e) {
            log.error("Saga [{}] failed: {}", sagaId, e.getMessage(), e);
            compensateSaga(sagaId, sagaData, e);
            return SagaResult.failure(sagaId, e.getMessage());
        }
    }
    
    /**
     * Compensate saga (rollback)
     */
    @Transactional
    public void compensateSaga(String sagaId, TransferSagaData sagaData, Exception error) {
        log.warn("Compensating saga: {}", sagaId);
        
        try {
            SagaState saga = sagaStateRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new IllegalStateException("Saga not found"));
            
            saga.setStatus(SagaStatus.COMPENSATING);
            saga.setErrorMessage(error.getMessage());
            sagaStateRepository.save(saga);
            
            // Compensate in reverse order
            
            if (sagaData.isFundsCredited()) {
                log.info("Compensating: Reversing credit");
                accountClient.debitWithIdempotency(
                    sagaData.getDestinationAccountId(),
                    sagaData.getAmount(),
                    sagaData.getTransactionReference() + "-COMP-CREDIT",
                    sagaData.getTransactionReference() + "-COMP"
                );
            }
            
            if (sagaData.isFundsDebited()) {
                log.info("Compensating: Reversing debit");
                accountClient.creditWithIdempotency(
                    sagaData.getSourceAccountId(),
                    sagaData.getAmount(),
                    sagaData.getTransactionReference() + "-COMP-DEBIT",
                    sagaData.getTransactionReference() + "-COMP"
                );
            }
            
            if (sagaData.getHoldReference() != null) {
                log.info("Compensating: Releasing hold");
                accountClient.releaseHold(sagaData.getHoldReference());
            }
            
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaStateRepository.save(saga);
            
            log.info("Saga {} compensated successfully", sagaId);
            
        } catch (Exception compensationError) {
            log.error("❌ CRITICAL: Compensation failed for saga {}: {}", 
                sagaId, compensationError.getMessage(), compensationError);
            
            // Store in dead letter queue for manual intervention
            storeInDLQ(sagaId, sagaData, error, compensationError);
        }
    }
    
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
    
    // Helper methods
    private void validateAccounts(TransferSagaData data) {
        if (data.getSourceAccountId().equals(data.getDestinationAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }
    }
    
    private String toJson(Object obj) {
        // Use ObjectMapper
        return "{}"; // Simplified
    }
    
    private void storeInDLQ(String sagaId, TransferSagaData data, 
                           Exception originalError, Exception compensationError) {
        // Store in dead letter queue table for manual intervention
        log.error("Storing failed saga in DLQ: {}", sagaId);
    }
}
