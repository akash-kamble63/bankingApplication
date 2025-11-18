package com.loan_service.service.implementation;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan_service.entity.OutboxEvent;
import com.loan_service.enums.OutboxStatus;
import com.loan_service.repository.OutboxEventRepository;
import com.loan_service.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	/**
	 * Save event to outbox in same transaction as business logic MANDATORY
	 * propagation ensures it runs in existing transaction
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
		try {
			String eventId = UUID.randomUUID().toString();
			String payloadJson = objectMapper.writeValueAsString(payload);

			OutboxEvent event = OutboxEvent.builder().eventId(eventId).aggregateType(aggregateType)
					.aggregateId(aggregateId).eventType(eventType).topic(topic).payload(payloadJson)
					.status(OutboxStatus.PENDING).retryCount(0).maxRetries(3).build();

			outboxEventRepository.save(event);
			log.debug("Loan service outbox event saved: eventId={}, type={}, aggregate={}", eventId, eventType,
					aggregateType);

		} catch (Exception e) {
			log.error("Failed to save outbox event: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to save outbox event", e);
		}
	}

	/**
	 * Mark event as published
	 */
	@Override
	@Transactional
	public void markAsPublished(Long eventId) {
		outboxEventRepository.findById(eventId).ifPresent(event -> {
			event.setStatus(OutboxStatus.PUBLISHED);
			event.setPublishedAt(LocalDateTime.now());
			outboxEventRepository.save(event);
			log.debug("Loan service outbox event published: id={}", eventId);
		});
	}

	/**
	 * Mark event as failed
	 */
	@Override
	@Transactional
	public void markAsFailed(Long eventId, String error) {
		outboxEventRepository.findById(eventId).ifPresent(event -> {
			event.incrementRetry();
			event.setLastError(error);

			if (event.getRetryCount() >= event.getMaxRetries()) {
				event.setStatus(OutboxStatus.FAILED);
				log.error("Loan service outbox event permanently failed: id={}, error={}", eventId, error);
			} else {
				log.warn("Loan service outbox event retry scheduled: id={}, attempt={}, nextRetry={}", eventId,
						event.getRetryCount(), event.getNextRetryAt());
			}

			outboxEventRepository.save(event);
		});
	}

	/**
	 * Cleanup old published events
	 */
	@Override
	@Transactional
	public int cleanupOldEvents(int retentionDays) {
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
		int deleted = outboxEventRepository.cleanupPublishedEvents(OutboxStatus.PUBLISHED, cutoffDate);
		log.info("Cleaned up {} published loan service outbox events older than {} days", deleted, retentionDays);
		return deleted;
	}
}
