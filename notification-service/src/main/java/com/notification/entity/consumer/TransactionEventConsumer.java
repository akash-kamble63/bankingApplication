package com.notification.entity.consumer;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.DTOs.NotificationRequest;
import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationPriority;
import com.notification.enums.NotificationType;
import com.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {
	private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "banking.transaction.events",
        groupId = "notification-service-transaction-group"
    )
    public void consumeTransactionEvent(String message) {
        try {
            log.info("Received transaction event: {}", message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            
            switch (eventType) {
                case "TransactionCompleted" -> handleTransactionCompleted(payload);
                case "LowBalanceAlert" -> handleLowBalanceAlert(payload);
                default -> log.warn("Unknown transaction event: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", e.getMessage(), e);
        }
    }

    private void handleTransactionCompleted(Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        String transactionRef = (String) payload.get("transactionReference");
        Object amountObj = payload.get("amount");
        String amount = amountObj != null ? amountObj.toString() : "0";
        String type = (String) payload.get("transactionType");
        
        NotificationRequest request = NotificationRequest.builder()
            .userId(userId)
            .referenceId(transactionRef)
            .type(NotificationType.TRANSACTION_COMPLETED)
            .channel(NotificationChannel.IN_APP)
            .priority(NotificationPriority.NORMAL)
            .subject("Transaction Completed")
            .content(String.format("%s transaction of ₹%s completed. Ref: %s", 
                type, amount, transactionRef))
            .build();
        
        notificationService.sendNotification(request);
    }

    private void handleLowBalanceAlert(Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        Object balanceObj = payload.get("balance");
        String balance = balanceObj != null ? balanceObj.toString() : "0";
        
        NotificationRequest request = NotificationRequest.builder()
            .userId(userId)
            .type(NotificationType.LOW_BALANCE_ALERT)
            .channel(NotificationChannel.SMS)
            .priority(NotificationPriority.HIGH)
            .subject("Low Balance Alert")
            .content(String.format("Your account balance is low: ₹%s", balance))
            .build();
        
        notificationService.sendNotification(request);
    }
}
