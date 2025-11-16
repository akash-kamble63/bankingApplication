package com.payment_service.configuration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.payment_service.entity.OutboxEvent;
import com.payment_service.enums.OutboxStatus;
import com.payment_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
	private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(OutboxStatus.PENDING);
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Publishing {} pending outbox events", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            try {
                event.setStatus(OutboxStatus.PROCESSING);
                outboxRepository.save(event);
                
                // Publish to Kafka
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            handlePublishFailure(event, ex);
                        } else {
                            handlePublishSuccess(event);
                        }
                    });
                
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                handlePublishFailure(event, e);
            }
        }
    }

    @Transactional
    private void handlePublishSuccess(OutboxEvent event) {
        event.setStatus(OutboxStatus.SENT);
        event.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(event);
        log.debug("Event published successfully: {}", event.getId());
    }

    @Transactional
    private void handlePublishFailure(OutboxEvent event, Throwable error) {
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(error.getMessage());
        outboxRepository.save(event);
        log.error("Event publish failed: {}", event.getId());
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        outboxRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.SENT, threshold);
        log.info("Cleaned up old outbox events");
    }
}
