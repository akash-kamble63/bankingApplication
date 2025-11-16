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
public class PaymentEventConsumer {
	private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "banking.payment.status",
        groupId = "notification-service-payment-group"
    )
    public void consumePaymentEvent(String message) {
        try {
            log.info("Received payment event: {}", message);
            
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            
            switch (eventType) {
                case "PaymentCompleted" -> handlePaymentCompleted(payload);
                case "PaymentFailed" -> handlePaymentFailed(payload);
                default -> log.warn("Unknown payment event type: {}", eventType);
            }
            
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCompleted(Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        String paymentReference = (String) payload.get("paymentReference");
        Object amountObj = payload.get("amount");
        String amount = amountObj != null ? amountObj.toString() : "0";
        String paymentMethod = (String) payload.get("paymentMethod");
        
        // Send multi-channel notification
        sendPaymentNotification(
            userId,
            paymentReference,
            NotificationType.PAYMENT_SUCCESS,
            "Payment Successful",
            String.format("Your payment of ₹%s via %s was successful. Reference: %s", 
                amount, paymentMethod, paymentReference)
        );
    }

    private void handlePaymentFailed(Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        String paymentReference = (String) payload.get("paymentReference");
        Object amountObj = payload.get("amount");
        String amount = amountObj != null ? amountObj.toString() : "0";
        String reason = (String) payload.getOrDefault("failureReason", "Unknown reason");
        
        sendPaymentNotification(
            userId,
            paymentReference,
            NotificationType.PAYMENT_FAILED,
            "Payment Failed",
            String.format("Your payment of ₹%s failed. Reason: %s. Reference: %s", 
                amount, reason, paymentReference)
        );
    }

    private void sendPaymentNotification(Long userId, String reference, 
                                        NotificationType type, String subject, 
                                        String content) {
        // Send EMAIL
        NotificationRequest emailRequest = NotificationRequest.builder()
            .userId(userId)
            .referenceId(reference)
            .type(type)
            .channel(NotificationChannel.EMAIL)
            .priority(NotificationPriority.HIGH)
            .subject(subject)
            .content(content)
            .build();
        
        notificationService.sendNotification(emailRequest);
        
        // Send SMS
        NotificationRequest smsRequest = NotificationRequest.builder()
            .userId(userId)
            .referenceId(reference)
            .type(type)
            .channel(NotificationChannel.SMS)
            .priority(NotificationPriority.HIGH)
            .subject(subject)
            .content(content)
            .build();
        
        notificationService.sendNotification(smsRequest);
        
        // Send IN_APP
        NotificationRequest inAppRequest = NotificationRequest.builder()
            .userId(userId)
            .referenceId(reference)
            .type(type)
            .channel(NotificationChannel.IN_APP)
            .priority(NotificationPriority.HIGH)
            .subject(subject)
            .content(content)
            .build();
        
        notificationService.sendNotification(inAppRequest);
    }
}
