package com.account_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.account_service.model.Beneficiary;
import com.account_service.model.SagaState;
import com.account_service.patterns.SagaOrchestrator;
import com.account_service.repository.BeneficiaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Saga-based service for complex beneficiary operations
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiarySagaService {

    private final SagaOrchestrator sagaOrchestrator;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Execute beneficiary verification saga
     * This involves: verify account exists, validate IFSC, mark verified, send
     * notification
     */
    @Transactional
    public String executeBeneficiaryVerificationSaga(Long beneficiaryId) {
        log.info("Starting beneficiary verification saga for beneficiary: {}", beneficiaryId);

        try {
            // Get beneficiary
            Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                    .orElseThrow(() -> new RuntimeException("Beneficiary not found: " + beneficiaryId));

            // Create saga data
            BeneficiaryVerificationSagaData sagaData = BeneficiaryVerificationSagaData.builder()
                    .beneficiaryId(beneficiaryId)
                    .userId(beneficiary.getUserId())
                    .accountNumber(beneficiary.getBeneficiaryAccountNumber())
                    .ifscCode(beneficiary.getBeneficiaryIfsc())
                    .beneficiaryName(beneficiary.getBeneficiaryName())
                    .build();

            // Start saga
            String sagaId = sagaOrchestrator.startSaga("BENEFICIARY_VERIFICATION", sagaData);

            // Execute saga steps
            executeBeneficiaryVerificationSteps(sagaId, sagaData);

            return sagaId;

        } catch (Exception e) {
            log.error("Failed to start beneficiary verification saga", e);
            throw new RuntimeException("Saga initiation failed", e);
        }
    }

    /**
     * Execute verification saga steps
     */
    private void executeBeneficiaryVerificationSteps(String sagaId, BeneficiaryVerificationSagaData data) {
        try {
            // Step 1: Verify account exists with external bank service
            sagaOrchestrator.updateSagaState(sagaId, "VERIFY_ACCOUNT_EXISTS",
                    SagaState.SagaStatus.PROCESSING, null);

            boolean accountExists = verifyAccountWithBank(data.getAccountNumber(), data.getIfscCode());
            if (!accountExists) {
                sagaOrchestrator.failSaga(sagaId, "Bank account verification failed");
                return;
            }
            sagaOrchestrator.updateSagaState(sagaId, "VERIFY_ACCOUNT_EXISTS",
                    SagaState.SagaStatus.PROCESSING, "VERIFY_ACCOUNT_EXISTS");

            // Step 2: Validate IFSC code
            sagaOrchestrator.updateSagaState(sagaId, "VALIDATE_IFSC",
                    SagaState.SagaStatus.PROCESSING, null);

            boolean ifscValid = validateIfscCode(data.getIfscCode());
            if (!ifscValid) {
                sagaOrchestrator.failSaga(sagaId, "IFSC code validation failed");
                return;
            }
            sagaOrchestrator.updateSagaState(sagaId, "VALIDATE_IFSC",
                    SagaState.SagaStatus.PROCESSING, "VALIDATE_IFSC");

            // Step 3: Mark beneficiary as verified
            sagaOrchestrator.updateSagaState(sagaId, "MARK_VERIFIED",
                    SagaState.SagaStatus.PROCESSING, null);

            markBeneficiaryVerified(data.getBeneficiaryId());
            sagaOrchestrator.updateSagaState(sagaId, "MARK_VERIFIED",
                    SagaState.SagaStatus.PROCESSING, "MARK_VERIFIED");

            // Step 4: Send confirmation notification
            sagaOrchestrator.updateSagaState(sagaId, "SEND_CONFIRMATION",
                    SagaState.SagaStatus.PROCESSING, null);

            sendVerificationConfirmation(data.getUserId(), data.getBeneficiaryName());
            sagaOrchestrator.updateSagaState(sagaId, "SEND_CONFIRMATION",
                    SagaState.SagaStatus.PROCESSING, "SEND_CONFIRMATION");

            // Complete saga
            sagaOrchestrator.completeSaga(sagaId);
            log.info("Beneficiary verification saga completed: {}", sagaId);

        } catch (Exception e) {
            log.error("Error executing beneficiary verification saga steps", e);
            sagaOrchestrator.failSaga(sagaId, e.getMessage());
        }
    }

    /**
     * Verify account exists with external bank service
     */
    private boolean verifyAccountWithBank(String accountNumber, String ifscCode) {
        // TODO: Call external bank verification API
        log.info("Verifying account with bank: accountNumber={}, ifsc={}", accountNumber, ifscCode);

        // Simulate external API call
        try {
            Thread.sleep(100); // Simulate network delay
            // For demo, return true. In production, call actual API
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Validate IFSC code
     */
    private boolean validateIfscCode(String ifscCode) {
        // TODO: Validate against IFSC master data or external service
        log.info("Validating IFSC code: {}", ifscCode);

        // Basic validation - IFSC format: 4 letters + 0 + 6 alphanumeric
        if (ifscCode == null || !ifscCode.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            return false;
        }

        // In production, validate against master data or external API
        return true;
    }

    /**
     * Mark beneficiary as verified
     */
    private void markBeneficiaryVerified(Long beneficiaryId) {
        beneficiaryRepository.findById(beneficiaryId).ifPresent(beneficiary -> {
            beneficiary.setIsVerified(true);
            beneficiary.setVerifiedAt(java.time.LocalDateTime.now());
            beneficiary.setStatus(com.account_service.enums.BeneficiaryStatus.VERIFIED);
            beneficiaryRepository.save(beneficiary);
            log.info("Beneficiary marked as verified: {}", beneficiaryId);
        });
    }

    /**
     * Send verification confirmation
     */
    private void sendVerificationConfirmation(Long userId, String beneficiaryName) {
        // TODO: Send notification via notification service
        log.info("Sending verification confirmation to user: {}, beneficiary: {}",
                userId, beneficiaryName);

        // In production, publish event or call notification service
    }

    /**
     * Saga data for beneficiary verification
     */
    @lombok.Data
    @lombok.Builder
    public static class BeneficiaryVerificationSagaData {
        private Long beneficiaryId;
        private Long userId;
        private String accountNumber;
        private String ifscCode;
        private String beneficiaryName;
    }

}
