package com.payment_service.service.implementation;


import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.entity.OutboxEvent;
import com.payment_service.enums.OutboxStatus;
import com.payment_service.repository.OutboxEventRepository;
import com.payment_service.service.OutboxService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService{
	private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, String eventType,
                         String topic, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID().toString())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .payload(payloadJson)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
            
            outboxRepository.save(event);
            log.debug("Outbox event saved: {} for {}", eventType, aggregateId);
            
        } catch (Exception e) {
            log.error("Failed to save outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Outbox save failed", e);
        }
    }
}
