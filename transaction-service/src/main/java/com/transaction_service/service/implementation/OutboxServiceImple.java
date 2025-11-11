package com.transaction_service.service.implementation;


import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction_service.entity.OutboxEvent;
import com.transaction_service.entity.OutboxEvent.OutboxStatus;
import com.transaction_service.repository.OutboxRepository;
import com.transaction_service.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImple implements OutboxService{
	private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
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
    
    @Transactional
    public void markAsPublished(Long eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            outboxRepository.save(event);
        });
    }
    
    @Transactional
    public void markAsFailed(Long eventId, String error) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.incrementRetry();
            event.setLastError(error);
            
            if (event.getRetryCount() >= event.getMaxRetries()) {
                event.setStatus(OutboxStatus.FAILED);
            }
            
            outboxRepository.save(event);
        });
    }
    
    @Transactional
    public int cleanupOldEvents(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        return outboxRepository.cleanupPublishedEvents(cutoffDate);
    }
}
