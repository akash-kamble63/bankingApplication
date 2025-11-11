package com.transaction_service.scheduler;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.transaction_service.entity.OutboxEvent;
import com.transaction_service.repository.OutboxRepository;
import com.transaction_service.service.OutboxService;

import jakarta.transaction.Transactional;
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
	 * Process pending outbox events every 5 seconds
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

	private void publishEvent(OutboxEvent event) {
		try {
			kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
					.whenComplete((result, ex) -> {
						if (ex == null) {
							outboxService.markAsPublished(event.getId());
						} else {
							outboxService.markAsFailed(event.getId(), ex.getMessage());
						}
					});

		} catch (Exception e) {
			outboxService.markAsFailed(event.getId(), e.getMessage());
		}
	}
}
