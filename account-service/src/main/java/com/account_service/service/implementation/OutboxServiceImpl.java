package com.account_service.service.implementation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.model.OutboxEvent;
import com.account_service.model.OutboxEvent.OutboxStatus;
import com.account_service.repository.OutboxRepository;
import com.account_service.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save event to outbox in same transaction as business logic
     * MANDATORY propagation ensures it runs in existing transaction
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId,
            String eventType, String topic, Object payload) {
        try {
            String eventId = UUID.randomUUID().toString();
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(3)
                    .build();

            outboxRepository.save(event);
            log.debug("Outbox event saved: eventId={}, type={}", eventId, eventType);

        } catch (Exception e) {
            log.error("Failed to save outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    /**
     * Mark event as published
     */
    @Transactional
    public void markAsPublished(Long eventId) {
        Optional<OutboxEvent> eventOpt = outboxRepository.findById(eventId);

        if (eventOpt.isPresent()) {
            OutboxEvent event = eventOpt.get();
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(event);
            log.debug("Marked event {} as published", eventId);
        } else {
            log.warn("Event {} not found when marking as published", eventId);
        }
    }

    /**
     * Mark event as failed
     */
    @Transactional
    public void markAsFailed(Long eventId, String error) {
        Optional<OutboxEvent> eventOpt = outboxRepository.findById(eventId);

        if (eventOpt.isPresent()) {
            OutboxEvent event = eventOpt.get();
            event.setLastError(error);

            if (event.getRetryCount() < event.getMaxRetries()) {
                event.incrementRetry();
                log.warn("Event {} failed, will retry. Attempt {}/{}",
                        eventId, event.getRetryCount(), event.getMaxRetries());
            } else {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Event {} permanently failed after {} retries",
                        eventId, event.getRetryCount());
            }

            outboxRepository.save(event);
        } else {
            log.warn("Event {} not found when marking as failed", eventId);
        }
    }

    /**
     * Cleanup old published events
     */
    @Transactional
    public int cleanupOldEvents(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = outboxRepository.deleteOldPublishedEvents(cutoffDate);
        log.info("Cleaned up {} old outbox events older than {} days", deleted, daysToKeep);
        return deleted;
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> getEvent(Long eventId) {
        return outboxRepository.findById(eventId);
    }

    @Transactional
    public void retryEvent(Long eventId) {
        Optional<OutboxEvent> eventOpt = outboxRepository.findById(eventId);

        if (eventOpt.isPresent()) {
            OutboxEvent event = eventOpt.get();
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setNextRetryAt(null);
            event.setLastError(null);
            outboxRepository.save(event);
            log.info("Event {} reset for manual retry", eventId);
        } else {
            log.warn("Event {} not found when attempting manual retry", eventId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OutboxHealthMetrics getHealthMetrics() {
        return new OutboxHealthMetricsImpl(
                outboxRepository.countByStatus(OutboxStatus.PENDING),
                outboxRepository.countByStatus(OutboxStatus.PUBLISHED),
                outboxRepository.countByStatus(OutboxStatus.FAILED),
                outboxRepository.countPermanentlyFailedEvents());
    }

    private record OutboxHealthMetricsImpl(
            long pendingCount,
            long publishedCount,
            long failedCount,
            long permanentlyFailedCount) implements OutboxHealthMetrics {
    }
}
