package com.transaction_service.service.implementation;



import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction_service.entity.TransactionEventStore;
import com.transaction_service.repository.EventStoreRepository;
import com.transaction_service.service.EventSourcingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSourcingServiceImpl implements EventSourcingService{
	private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void storeEvent(String aggregateId, String eventType, Object eventData, 
                          Long userId, String correlationId, String causationId) {
        try {
            Long version = getNextVersion(aggregateId);
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            Map<String, Object> metadata = Map.of(
                "userId", userId != null ? userId : 0L,
                "timestamp", LocalDateTime.now(),
                "source", "transaction-service"
            );
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            TransactionEventStore event = TransactionEventStore.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateId(aggregateId)
                .aggregateType("TRANSACTION")
                .eventType(eventType)
                .version(version)
                .eventData(eventDataJson)
                .metadata(metadataJson)
                .userId(userId)
                .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .causationId(causationId)
                .build();
            
            eventStoreRepository.save(event);
            
        } catch (Exception e) {
            log.error("Failed to store event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store event", e);
        }
    }
    
    private Long getNextVersion(String aggregateId) {
        Optional<Long> latestVersion = eventStoreRepository.findLatestVersion(aggregateId);
        return latestVersion.map(v -> v + 1).orElse(1L);
    }
}
