package com.fraud_detection.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.fraud_detection.dto.FraudCheckResponseDTO;
import com.fraud_detection.dto.FraudEventDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventProducer {
private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.fraud-check-completed:fraud-check-completed}")
    private String fraudCheckCompletedTopic;
    
    @Value("${kafka.topic.fraud-alert:fraud-alert}")
    private String fraudAlertTopic;
    
    public void publishFraudCheckCompleted(FraudCheckResponseDTO response) {
        log.info("Publishing fraud check completed event for transaction: {}", 
                response.getTransactionId());
        
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(fraudCheckCompletedTopic, response.getTransactionId(), response);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Fraud check event sent successfully for transaction: {} to partition: {}", 
                        response.getTransactionId(), result.getRecordMetadata().partition());
            } else {
                log.error("Failed to send fraud check event for transaction: {}", 
                        response.getTransactionId(), ex);
            }
        });
        
        // If fraud detected, also send alert
        if (response.getStatus().name().equals("BLOCKED") || 
            response.getStatus().name().equals("MANUAL_REVIEW")) {
            publishFraudAlert(response);
        }
    }
    
    private void publishFraudAlert(FraudCheckResponseDTO response) {
        log.info("Publishing fraud alert for transaction: {}", response.getTransactionId());
        
        FraudEventDTO alert = FraudEventDTO.builder()
                .transactionId(response.getTransactionId())
                .status(response.getStatus())
                .riskScore(response.getRiskScore())
                .fraudReasons(response.getFraudReasons())
                .eventTime(response.getCheckedAt())
                .eventType("FRAUD_ALERT")
                .build();
        
        kafkaTemplate.send(fraudAlertTopic, response.getTransactionId(), alert);
    }
}
