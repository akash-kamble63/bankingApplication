package com.account_service.patterns;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.SagaState;
import com.account_service.model.SagaState.SagaStatus;
import com.account_service.repository.SagaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaScheduler {
    private final SagaRepository sagaRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final AccountMetrics metrics;
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
                    .compensationData("{}")
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            sagaRepository.save(saga);

            log.info("Saga started: sagaId={}, type={}", sagaId, sagaType);
            metrics.recordSagaOperation(sagaType, "STARTED");

            return sagaId;

        } catch (Exception e) {
            log.error("Failed to start saga: {}", e.getMessage(), e);
            metrics.recordSagaOperation(sagaType, "START_FAILED");
            throw new RuntimeException("Failed to start saga", e);
        }
    }

    /**
     * Retry failed sagas every 5 minutes
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes
    @Transactional
    public void retryFailedSagas() {
        try {
            LocalDateTime retryTime = LocalDateTime.now().minusMinutes(5);
            List<SagaState> failedSagas = sagaRepository.findFailedSagasForRetry(retryTime);

            if (!failedSagas.isEmpty()) {
                log.info("Found {} failed sagas to retry", failedSagas.size());

                for (SagaState saga : failedSagas) {
                    try {
                        log.info("Retrying saga: {}, attempt {}/{}",
                                saga.getSagaId(),
                                saga.getRetryCount() + 1,
                                saga.getMaxRetries());

                        sagaOrchestrator.retryFailedSagas();
                        metrics.recordSagaOperation(saga.getSagaType(), "RETRY_SUCCESS");

                    } catch (Exception e) {
                        log.error("Failed to retry saga: {}", saga.getSagaId(), e);
                        metrics.recordSagaOperation(saga.getSagaType(), "RETRY_FAILED");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error in saga retry scheduler", e);
        }
    }

    /**
     * Monitor stuck sagas every 10 minutes
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000) // 10 minutes
    @Transactional
    public void monitorStuckSagas() {
        try {
            // Sagas stuck in PROCESSING for more than 30 minutes
            LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(30);
            List<SagaState> stuckSagas = sagaRepository.findStuckSagas(stuckThreshold);

            if (!stuckSagas.isEmpty()) {
                log.error("ALERT: Found {} stuck sagas!", stuckSagas.size());

                for (SagaState saga : stuckSagas) {
                    log.error("Stuck saga detected: id={}, type={}, step={}, age={}min",
                            saga.getSagaId(),
                            saga.getSagaType(),
                            saga.getCurrentStep(),
                            java.time.Duration.between(saga.getUpdatedAt(), LocalDateTime.now()).toMinutes());

                    // Mark as failed and start compensation
                    sagaOrchestrator.failSaga(saga.getSagaId(),
                            "Saga stuck in processing for too long");

                    metrics.recordSagaOperation(saga.getSagaType(), "STUCK_DETECTED");
                }
            }

        } catch (Exception e) {
            log.error("Error in stuck saga monitor", e);
        }
    }

    /**
     * Health monitoring - every minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000) // 1 minute
    public void monitorSagaHealth() {
        try {
            long processingCount = sagaRepository.countByStatus(SagaStatus.PROCESSING);
            long compensatingCount = sagaRepository.countByStatus(SagaStatus.COMPENSATING);
            long failedCount = sagaRepository.countByStatus(SagaStatus.FAILED);

            // Log metrics
            log.debug("Saga health: processing={}, compensating={}, failed={}",
                    processingCount, compensatingCount, failedCount);

            // Alert on critical thresholds
            if (processingCount > 100) {
                log.warn("HIGH LOAD: {} sagas currently processing", processingCount);
            }

            if (compensatingCount > 10) {
                log.error("ALERT: {} sagas currently compensating!", compensatingCount);
            }

            if (failedCount > 50) {
                log.error("CRITICAL: {} failed sagas detected!", failedCount);
            }

            // Find permanently failed sagas
            List<SagaState> permanentlyFailed = sagaRepository.findPermanentlyFailedSagas();
            if (!permanentlyFailed.isEmpty()) {
                log.error("CRITICAL: {} sagas permanently failed (exceeded max retries)",
                        permanentlyFailed.size());

                for (SagaState saga : permanentlyFailed) {
                    log.error("Permanently failed saga: id={}, type={}, error={}",
                            saga.getSagaId(),
                            saga.getSagaType(),
                            saga.getErrorMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error in saga health monitor", e);
        }
    }

    /**
     * Cleanup old completed sagas - daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldSagas() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30); // Keep 30 days

            int deletedCount = 0;
            List<SagaState> oldSagas = sagaRepository.findByCreatedAtBetween(
                    LocalDateTime.MIN, cutoffDate);

            for (SagaState saga : oldSagas) {
                if (saga.getStatus() == SagaStatus.COMPLETED) {
                    sagaRepository.delete(saga);
                    deletedCount++;
                }
            }

            log.info("Saga cleanup completed: {} old completed sagas deleted", deletedCount);

        } catch (Exception e) {
            log.error("Error in saga cleanup", e);
        }
    }
}
