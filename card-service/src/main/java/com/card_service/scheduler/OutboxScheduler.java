package com.card_service.scheduler;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.card_service.entity.OutboxEvent;
import com.card_service.entity.OutboxEvent.OutboxStatus;
import com.card_service.repository.OutboxRepository;
import com.card_service.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
	private final OutboxRepository outboxRepository;
	private final OutboxService outboxService;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * Process pending outbox events every 5 seconds This ensures eventual
	 * consistency even if app crashes after DB commit
	 */
	@Scheduled(fixedDelay = 5000, initialDelay = 10000)
	@Transactional
	public void processOutboxEvents() {
		try {
			List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(100);

			if (!pendingEvents.isEmpty()) {
				log.info("Processing {} pending outbox events", pendingEvents.size());

				for (OutboxEvent event : pendingEvents) {
					publishEvent(event);
				}
			}

		} catch (Exception e) {
			log.error("Error processing outbox events: {}", e.getMessage(), e);
		}
	}

	/**
	 * Cleanup old published events daily
	 */
	@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
	public void cleanupOldEvents() {
		try {
			int deleted = outboxService.cleanupOldEvents(7); // Keep 7 days
			log.info("Outbox cleanup completed: {} events deleted", deleted);
		} catch (Exception e) {
			log.error("Error cleaning up outbox events: {}", e.getMessage(), e);
		}
	}

	/**
	 * Publish event to Kafka
	 */
	private void publishEvent(OutboxEvent event) {
		try {
			kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
					.whenComplete((result, ex) -> {
						if (ex == null) {
							outboxService.markAsPublished(event.getId());
							log.debug("Event published: id={}, topic={}", event.getId(), event.getTopic());
						} else {
							outboxService.markAsFailed(event.getId(), ex.getMessage());
							log.error("Failed to publish event: id={}, error={}", event.getId(), ex.getMessage());
						}
					});

		} catch (Exception e) {
			outboxService.markAsFailed(event.getId(), e.getMessage());
			log.error("Exception publishing event: id={}, error={}", event.getId(), e.getMessage(), e);
		}
	}

	@Scheduled(fixedDelay = 60000) // Every minute
	public void monitorOutboxHealth() {
		long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);

		if (pendingCount > 1000) { // Alert threshold
			log.error("CRITICAL: {} pending outbox events! Kafka may be down", pendingCount);
		}

		long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);
		if (failedCount > 100) {
			log.error("CRITICAL: {} permanently failed events!", failedCount);
		}
	}
}
