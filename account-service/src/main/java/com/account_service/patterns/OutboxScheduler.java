package com.account_service.patterns;

import java.time.LocalDateTime;
import java.util.List;

import javax.management.monitor.Monitor;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.OutboxEvent;
import com.account_service.model.OutboxEvent.OutboxStatus;
import com.account_service.repository.OutboxRepository;
import com.account_service.service.OutboxService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreaker kafkaCircuitBreaker;
    private final MeterRegistry meterRegistry;

    /**
     * Process pending outbox events every 5 seconds
     * This ensures eventual consistency even if app crashes after DB commit
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(100);

            if (!pendingEvents.isEmpty()) {
                log.info("Processing {} pending outbox events", pendingEvents.size());

                int successCount = 0;
                int failureCount = 0;

                for (OutboxEvent event : pendingEvents) {
                    boolean success = publishEvent(event);
                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }

                log.info("Outbox processing completed: {} succeeded, {} failed",
                        successCount, failureCount);

                // Record metrics
                recordOutboxMetrics(successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("Error processing outbox events: {}", e.getMessage(), e);
            recordOutboxError();
        }
    }

    /**
     * Process failed events that are ready for retry
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 20000) // Every 30 seconds
    @Transactional
    public void retryFailedEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OutboxEvent> retryableEvents = outboxRepository
                    .findRetryableEvents(now, 50);

            if (!retryableEvents.isEmpty()) {
                log.info("Retrying {} failed outbox events", retryableEvents.size());

                for (OutboxEvent event : retryableEvents) {
                    publishEvent(event);
                }
            }

        } catch (Exception e) {
            log.error("Error retrying failed events: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup old published events daily
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        try {
            int deleted = outboxService.cleanupOldEvents(7); // Keep 7 days
            log.info("Outbox cleanup completed: {} events deleted", deleted);

            // Record cleanup metric
            meterRegistry.counter("outbox.cleanup.events.deleted").increment(deleted);

        } catch (Exception e) {
            log.error("Error cleaning up outbox events: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish event to Kafka with circuit breaker protection
     */
    private boolean publishEvent(OutboxEvent event) {
        try {
            // Use circuit breaker for Kafka operations
            kafkaCircuitBreaker.executeSupplier(() -> {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outboxService.markAsPublished(event.getId());
                                log.debug("Event published: id={}, topic={}, partition={}, offset={}",
                                        event.getId(),
                                        event.getTopic(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                outboxService.markAsFailed(event.getId(), ex.getMessage());
                                log.error("Failed to publish event: id={}, error={}",
                                        event.getId(), ex.getMessage());
                            }
                        });
                return true;
            });

            return true;

        } catch (Exception e) {
            outboxService.markAsFailed(event.getId(), e.getMessage());
            log.error("Circuit breaker open or exception publishing event: id={}, error={}",
                    event.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Monitor outbox health every minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000) // Every minute
    public void monitorOutboxHealth() {
        try {
            long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);
            long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);
            long permanentlyFailedCount = outboxRepository.countPermanentlyFailedEvents();

            // Record gauge metrics
            meterRegistry.gauge("outbox.events.pending", pendingCount);
            meterRegistry.gauge("outbox.events.failed", failedCount);
            meterRegistry.gauge("outbox.events.permanently.failed", permanentlyFailedCount);

            // Alert on critical thresholds
            if (pendingCount > 1000) {
                log.error("CRITICAL: {} pending outbox events! Kafka may be down or slow", pendingCount);
                meterRegistry.counter("outbox.alerts.pending.threshold").increment();
            }

            if (failedCount > 100) {
                log.error("CRITICAL: {} failed outbox events!", failedCount);
                meterRegistry.counter("outbox.alerts.failed.threshold").increment();
            }

            if (permanentlyFailedCount > 50) {
                log.error("CRITICAL: {} permanently failed events need manual intervention!",
                        permanentlyFailedCount);
                meterRegistry.counter("outbox.alerts.permanently.failed.threshold").increment();
            }

            log.debug("Outbox health: pending={}, failed={}, permanently_failed={}",
                    pendingCount, failedCount, permanentlyFailedCount);

        } catch (Exception e) {
            log.error("Error monitoring outbox health: {}", e.getMessage(), e);
        }
    }

    /**
     * Record outbox processing metrics
     */
    private void recordOutboxMetrics(int successCount, int failureCount) {
        meterRegistry.counter("outbox.processing.success").increment(successCount);
        meterRegistry.counter("outbox.processing.failure").increment(failureCount);

        if (successCount > 0) {
            meterRegistry.counter("outbox.processing.batches.success").increment();
        }
        if (failureCount > 0) {
            meterRegistry.counter("outbox.processing.batches.partial_failure").increment();
        }
    }

    /**
     * Record outbox processing errors
     */
    private void recordOutboxError() {
        meterRegistry.counter("outbox.processing.errors").increment();
    }

}