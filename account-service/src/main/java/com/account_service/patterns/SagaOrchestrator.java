package com.account_service.patterns;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.SagaState;
import com.account_service.model.SagaState.SagaStatus;
import com.account_service.repository.SagaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor

public class SagaOrchestrator {
	private final SagaRepository sagaRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Start a new saga
     */
    @Transactional
    public String startSaga(String sagaType, Object sagaData) {
        try {
            String sagaId = UUID.randomUUID().toString();
            String payload = objectMapper.writeValueAsString(sagaData);
            
            SagaState saga = SagaState.builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .status(SagaStatus.STARTED)
                .currentStep("INITIAL")
                .completedSteps("[]")
                .payload(payload)
                .retryCount(0)
                .maxRetries(3)
                .build();
            
            sagaRepository.save(saga);
            log.info("Saga started: sagaId={}, type={}", sagaId, sagaType);
            
            return sagaId;
            
        } catch (Exception e) {
            log.error("Failed to start saga: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start saga", e);
        }
    }
    
    /**
     * Update saga state
     */
    @Transactional
    public void updateSagaState(String sagaId, String currentStep, 
                               SagaStatus status, String completedSteps) {
        sagaRepository.findBySagaId(sagaId).ifPresent(saga -> {
            saga.setCurrentStep(currentStep);
            saga.setStatus(status);
            saga.setCompletedSteps(completedSteps);
            sagaRepository.save(saga);
            log.debug("Saga updated: sagaId={}, step={}, status={}", sagaId, currentStep, status);
        });
    }
    
    /**
     * Complete saga successfully
     */
    @Transactional
    public void completeSaga(String sagaId) {
        sagaRepository.findBySagaId(sagaId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCompletedAt(LocalDateTime.now());
            sagaRepository.save(saga);
            log.info("Saga completed: sagaId={}", sagaId);
        });
    }
    
    /**
     * Fail saga and start compensation
     */
    @Transactional
    public void failSaga(String sagaId, String errorMessage) {
        sagaRepository.findBySagaId(sagaId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.COMPENSATING);
            saga.setErrorMessage(errorMessage);
            saga.setRetryCount(saga.getRetryCount() + 1);
            sagaRepository.save(saga);
            log.error("Saga failed: sagaId={}, error={}", sagaId, errorMessage);
            
            // Start compensation
            compensateSaga(saga);
        });
    }
    
    /**
     * Compensate saga (rollback)
     */
    @Transactional
    public void compensateSaga(SagaState saga) {
        try {
            log.info("Starting compensation for saga: {}", saga.getSagaId());
            
            // Parse completed steps
            List<String> completedSteps = objectMapper.readValue(
                saga.getCompletedSteps(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            
            // Execute compensation in reverse order
            for (int i = completedSteps.size() - 1; i >= 0; i--) {
                String step = completedSteps.get(i);
                log.info("Compensating step: {} for saga: {}", step, saga.getSagaId());
                executeCompensation(saga.getSagaType(), step, saga.getPayload());
            }
            
            saga.setStatus(SagaStatus.COMPENSATED);
            sagaRepository.save(saga);
            log.info("Saga compensated: {}", saga.getSagaId());
            
        } catch (Exception e) {
            log.error("Compensation failed for saga: {}", saga.getSagaId(), e);
            saga.setStatus(SagaStatus.FAILED);
            saga.setErrorMessage("Compensation failed: " + e.getMessage());
            sagaRepository.save(saga);
        }
    }
    
    /**
     * Execute compensation for specific step
     */
    private void executeCompensation(String sagaType, String step, String payload) {
        // Implement compensation logic based on saga type and step
        switch (sagaType) {
            case "FUND_TRANSFER":
                compensateFundTransferStep(step, payload);
                break;
            case "ACCOUNT_CREATION":
                compensateAccountCreationStep(step, payload);
                break;
            default:
                log.warn("Unknown saga type for compensation: {}", sagaType);
        }
    }
    
    /**
     * Compensate fund transfer steps
     */
    private void compensateFundTransferStep(String step, String payload) {
        switch (step) {
            case "RESERVE_FUNDS":
                log.info("Releasing fund hold");
                // Call account service to release hold
                break;
            case "DEBIT_ACCOUNT":
                log.info("Reversing debit");
                // Call account service to credit back
                break;
            case "CREDIT_ACCOUNT":
                log.info("Reversing credit");
                // Call account service to debit back
                break;
            default:
                log.warn("No compensation needed for step: {}", step);
        }
    }
    
    /**
     * Compensate account creation steps
     */
    private void compensateAccountCreationStep(String step, String payload) {
        switch (step) {
            case "CREATE_ACCOUNT":
                log.info("Deleting created account");
                // Mark account as deleted
                break;
            case "ASSIGN_CARD":
                log.info("Canceling card assignment");
                // Cancel card
                break;
            default:
                log.warn("No compensation needed for step: {}", step);
        }
    }
    
    /**
     * Get saga state
     */
    @Transactional(readOnly = true)
    public SagaState getSagaState(String sagaId) {
        return sagaRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));
    }
    
    /**
     * Retry failed sagas
     */
    @Transactional
    public void retryFailedSagas() {
        LocalDateTime retryTime = LocalDateTime.now().minusMinutes(5);
        List<SagaState> failedSagas = sagaRepository.findFailedSagasForRetry(retryTime);
        
        log.info("Retrying {} failed sagas", failedSagas.size());
        
        for (SagaState saga : failedSagas) {
            if (saga.getRetryCount() < saga.getMaxRetries()) {
                saga.setStatus(SagaStatus.PROCESSING);
                saga.setRetryCount(saga.getRetryCount() + 1);
                sagaRepository.save(saga);
                log.info("Retrying saga: {}, attempt {}", saga.getSagaId(), saga.getRetryCount());
                // Trigger saga execution
            }
        }
    }
}
