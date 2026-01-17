package com.account_service.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish notification event to be consumed by notification-service
     */
    public void publishNotificationEvent(Long userId, String type, String title,
            String message, String priority,
            Map<String, String> metadata) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "NotificationRequested");
            event.put("aggregateId", "USER-" + userId);
            event.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("type", type);
            payload.put("title", title);
            payload.put("message", message);
            payload.put("priority", priority);
            payload.put("metadata", metadata);
            payload.put("sendEmail", true);
            payload.put("sendPush", true);
            payload.put("sendSms", false);

            event.put("payload", payload);

            publishEvent("banking.notification.events", "USER-" + userId, event);
            log.info("Published notification event: type={}, userId={}", type, userId);

        } catch (Exception e) {
            log.error("Failed to publish notification event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish fund transfer failed notification
     */
    public void publishTransferFailedNotification(Long userId, String sourceAccount,
            String destinationAccount, String amount) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("sourceAccount", sourceAccount);
        metadata.put("destinationAccount", destinationAccount);
        metadata.put("amount", amount);

        String message = String.format(
                "Your transfer of ₹%s from account %s to account %s has failed. " +
                        "The amount has been credited back to your account.",
                amount, maskAccount(sourceAccount), maskAccount(destinationAccount));

        publishNotificationEvent(userId, "FUND_TRANSFER_FAILED", "Transfer Failed",
                message, "HIGH", metadata);
    }

    /**
     * Publish beneficiary verification failed notification
     */
    public void publishVerificationFailedNotification(Long userId, String beneficiaryName,
            String accountNumber) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("beneficiaryName", beneficiaryName);
        metadata.put("accountNumber", accountNumber);
        metadata.put("reason", "Saga compensation");

        String message = String.format(
                "Verification failed for beneficiary '%s' (Account: %s). " +
                        "The verification has been rolled back.",
                beneficiaryName, maskAccount(accountNumber));

        publishNotificationEvent(userId, "BENEFICIARY_VERIFICATION_FAILED",
                "Beneficiary Verification Failed",
                message, "MEDIUM", metadata);
    }

    /**
     * Common method to publish events to Kafka
     */
    private void publishEvent(String topic, String key, Map<String, Object> event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Event sent to {} partition {} offset {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Mask account number for security
     */
    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
