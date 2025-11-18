package com.loan_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loan_service.entity.OutboxEvent;
import com.loan_service.enums.OutboxStatus;
import com.loan_service.repository.OutboxEventRepository;
import com.loan_service.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {
	private final OutboxEventRepository outboxEventRepository;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
            .findPendingEventsReadyForRetry(
                OutboxStatus.PENDING, 
                LocalDateTime.now()
            );
        
        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            outboxService.markAsFailed(event.getId(), ex.getMessage());
                        } else {
                            outboxService.markAsPublished(event.getId());
                        }
                    });
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
                outboxService.markAsFailed(event.getId(), e.getMessage());
            }
        }
    }
    
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    public void cleanupOldEvents() {
        outboxService.cleanupOldEvents(7); // Keep 7 days
    }
}
