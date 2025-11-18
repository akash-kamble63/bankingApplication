package com.card_service.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.card_service.entity.Card;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardEventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Publish Card Issued Event
	 */
	public void publishCardIssued(Card card) {
		try {
			Map<String, Object> event = new HashMap<>();
			event.put("eventType", "CardIssued");
			event.put("aggregateId", card.getCardReference());
			event.put("timestamp", LocalDateTime.now().toString());

			Map<String, Object> payload = new HashMap<>();
			payload.put("cardReference", card.getCardReference());
			payload.put("userId", card.getUserId());
			payload.put("accountId", card.getAccountId());
			payload.put("cardToken", card.getCardToken());
			payload.put("cardType", card.getCardType().name());
			payload.put("cardNetwork", card.getCardNetwork().name());
			payload.put("status", card.getStatus().name());
			payload.put("isVirtual", card.getIsVirtual());

			event.put("payload", payload);

			publishEvent("banking.card.issued", card.getCardReference(), event);
			log.info("Published CardIssued event for card: {}", card.getCardReference());

		} catch (Exception e) {
			log.error("Failed to publish CardIssued event: {}", e.getMessage(), e);
		}
	}

	/**
	 * Publish Card Activated Event
	 */
	public void publishCardActivated(Card card) {
		try {
			Map<String, Object> event = buildCardEvent(card, "CardActivated");
			publishEvent("banking.card.activated", card.getCardReference(), event);
			log.info("Published CardActivated event for card: {}", card.getCardReference());
		} catch (Exception e) {
			log.error("Failed to publish CardActivated event: {}", e.getMessage(), e);
		}
	}

	/**
	 * Publish Card Blocked Event
	 */
	public void publishCardBlocked(Card card, String reason) {
		try {
			Map<String, Object> event = buildCardEvent(card, "CardBlocked");
			Map<String, Object> payload = (Map<String, Object>) event.get("payload");
			payload.put("blockReason", reason);

			publishEvent("banking.card.blocked", card.getCardReference(), event);
			log.info("Published CardBlocked event for card: {}", card.getCardReference());
		} catch (Exception e) {
			log.error("Failed to publish CardBlocked event: {}", e.getMessage(), e);
		}
	}

	/**
	 * Publish Card Frozen Event
	 */
	public void publishCardFrozen(Card card) {
		try {
			Map<String, Object> event = buildCardEvent(card, "CardFrozen");
			publishEvent("banking.card.events", card.getCardReference(), event);
		} catch (Exception e) {
			log.error("Failed to publish CardFrozen event: {}", e.getMessage(), e);
		}
	}

	/**
	 * Publish Card Transaction Event
	 */
	public void publishCardTransaction(String cardReference, String transactionType, java.math.BigDecimal amount,
			String merchantName) {
		try {
			Map<String, Object> event = new HashMap<>();
			event.put("eventType", "CardTransactionProcessed");
			event.put("aggregateId", cardReference);
			event.put("timestamp", LocalDateTime.now().toString());

			Map<String, Object> payload = new HashMap<>();
			payload.put("cardReference", cardReference);
			payload.put("transactionType", transactionType);
			payload.put("amount", amount);
			payload.put("merchantName", merchantName);

			event.put("payload", payload);

			publishEvent("banking.card.transaction", cardReference, event);

		} catch (Exception e) {
			log.error("Failed to publish CardTransaction event: {}", e.getMessage(), e);
		}
	}

	/**
	 * Build standardized card event
	 */
	private Map<String, Object> buildCardEvent(Card card, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("eventType", eventType);
		event.put("aggregateId", card.getCardReference());
		event.put("timestamp", LocalDateTime.now().toString());

		Map<String, Object> payload = new HashMap<>();
		payload.put("cardReference", card.getCardReference());
		payload.put("userId", card.getUserId());
		payload.put("accountId", card.getAccountId());
		payload.put("cardToken", card.getCardToken());
		payload.put("cardType", card.getCardType().name());
		payload.put("status", card.getStatus().name());

		event.put("payload", payload);
		return event;
	}

	/**
	 * Generic publish method
	 */
	private void publishEvent(String topic, String key, Map<String, Object> event) {
		CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

		future.whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
			} else {
				log.debug("Event sent to {} partition {} offset {}", topic, result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			}
		});
	}
}
