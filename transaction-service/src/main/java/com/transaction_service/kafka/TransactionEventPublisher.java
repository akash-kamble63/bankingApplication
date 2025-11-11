package com.transaction_service.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish transaction event
     * Async and non-blocking
     * With callback handling
     */
    public CompletableFuture<SendResult<String, Object>> publishTransactionEvent(
            String topic, String key, Object event) {
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published: topic={}, partition={}, offset={}", 
                    topic, 
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: topic={}, error={}", 
                    topic, ex.getMessage(), ex);
            }
        });
        
        return future;
    }
}
