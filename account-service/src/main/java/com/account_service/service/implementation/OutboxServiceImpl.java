package com.account_service.service.implementation;


import java.time.LocalDateTime;
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
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(event);
            log.debug("Outbox event published: id={}", eventId);
        });
    }
    
    /**
     * Mark event as failed
     */
    @Transactional
    public void markAsFailed(Long eventId, String error) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.incrementRetry();
            event.setLastError(error);
            
            if (event.getRetryCount() >= event.getMaxRetries()) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event permanently failed: id={}, error={}", eventId, error);
            } else {
                log.warn("Outbox event retry scheduled: id={}, attempt={}, nextRetry={}", 
                    eventId, event.getRetryCount(), event.getNextRetryAt());
            }
            
            outboxRepository.save(event);
        });
    }
    
    /**
     * Cleanup old published events
     */
    @Transactional
    public int cleanupOldEvents(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        int deleted = outboxRepository.cleanupPublishedEvents(cutoffDate);
        log.info("Cleaned up {} published outbox events older than {} days", deleted, retentionDays);
        return deleted;
    }
}
