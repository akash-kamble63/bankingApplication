package com.account_service.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.account_service.model.Beneficiary;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish beneficiary created event
     */
    public void publishBeneficiaryCreated(Beneficiary beneficiary) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BeneficiaryCreated");
            event.put("aggregateId", "BENEFICIARY-" + beneficiary.getId());
            event.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("beneficiaryId", beneficiary.getId());
            payload.put("userId", beneficiary.getUserId());
            payload.put("accountId", beneficiary.getAccountId());
            payload.put("beneficiaryName", beneficiary.getBeneficiaryName());
            payload.put("beneficiaryAccountNumber", beneficiary.getBeneficiaryAccountNumber());
            payload.put("beneficiaryIfsc", beneficiary.getBeneficiaryIfsc());
            payload.put("beneficiaryBank", beneficiary.getBeneficiaryBank());
            payload.put("nickname", beneficiary.getNickname());
            payload.put("status", beneficiary.getStatus().name());
            payload.put("isVerified", beneficiary.getIsVerified());

            event.put("payload", payload);

            publishEvent("banking.beneficiary.created", "BENEFICIARY-" + beneficiary.getId(), event);
            log.info("Published BeneficiaryCreated event for beneficiary: {}", beneficiary.getId());

        } catch (Exception e) {
            log.error("Failed to publish BeneficiaryCreated event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish beneficiary verified event
     */
    public void publishBeneficiaryVerified(Beneficiary beneficiary) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BeneficiaryVerified");
            event.put("aggregateId", "BENEFICIARY-" + beneficiary.getId());
            event.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("beneficiaryId", beneficiary.getId());
            payload.put("userId", beneficiary.getUserId());
            payload.put("accountId", beneficiary.getAccountId());
            payload.put("beneficiaryName", beneficiary.getBeneficiaryName());
            payload.put("beneficiaryAccountNumber", beneficiary.getBeneficiaryAccountNumber());
            payload.put("verifiedAt", beneficiary.getVerifiedAt().toString());
            payload.put("status", beneficiary.getStatus().name());

            event.put("payload", payload);

            publishEvent("banking.beneficiary.verified", "BENEFICIARY-" + beneficiary.getId(), event);
            log.info("Published BeneficiaryVerified event for beneficiary: {}", beneficiary.getId());

        } catch (Exception e) {
            log.error("Failed to publish BeneficiaryVerified event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish beneficiary deleted event
     */
    public void publishBeneficiaryDeleted(Beneficiary beneficiary) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BeneficiaryDeleted");
            event.put("aggregateId", "BENEFICIARY-" + beneficiary.getId());
            event.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("beneficiaryId", beneficiary.getId());
            payload.put("userId", beneficiary.getUserId());
            payload.put("accountId", beneficiary.getAccountId());
            payload.put("beneficiaryAccountNumber", beneficiary.getBeneficiaryAccountNumber());
            payload.put("deletedAt", LocalDateTime.now().toString());

            event.put("payload", payload);

            publishEvent("banking.beneficiary.deleted", "BENEFICIARY-" + beneficiary.getId(), event);
            log.info("Published BeneficiaryDeleted event for beneficiary: {}", beneficiary.getId());

        } catch (Exception e) {
            log.error("Failed to publish BeneficiaryDeleted event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish beneficiary status changed event (for blocking, unblocking, etc.)
     */
    public void publishBeneficiaryStatusChanged(Beneficiary beneficiary, String oldStatus) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BeneficiaryStatusChanged");
            event.put("aggregateId", "BENEFICIARY-" + beneficiary.getId());
            event.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("beneficiaryId", beneficiary.getId());
            payload.put("userId", beneficiary.getUserId());
            payload.put("beneficiaryAccountNumber", beneficiary.getBeneficiaryAccountNumber());
            payload.put("oldStatus", oldStatus);
            payload.put("newStatus", beneficiary.getStatus().name());
            payload.put("changedAt", LocalDateTime.now().toString());

            event.put("payload", payload);

            publishEvent("banking.beneficiary.status.changed", "BENEFICIARY-" + beneficiary.getId(), event);
            log.info("Published BeneficiaryStatusChanged event for beneficiary: {}", beneficiary.getId());

        } catch (Exception e) {
            log.error("Failed to publish BeneficiaryStatusChanged event: {}", e.getMessage(), e);
        }
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

}
