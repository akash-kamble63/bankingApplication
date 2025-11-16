package com.transaction_service.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transaction_service.entity.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	/**
	 * Publish Transaction Initiated Event
	 */
	public CompletableFuture<SendResult<String, Object>> publishTransactionInitiated(Transaction transaction) {
		Map<String, Object> event = buildTransactionEvent(transaction, "TransactionInitiated");
		return publishEvent("banking.transaction.initiated", transaction.getTransactionReference(), event);
	}

	/**
	 * Publish Transaction Completed Event
	 */
	public CompletableFuture<SendResult<String, Object>> publishTransactionCompleted(Transaction transaction) {
		Map<String, Object> event = buildTransactionEvent(transaction, "TransactionCompleted");
		return publishEvent("banking.transaction.completed", transaction.getTransactionReference(), event);
	}

	/**
	 * Publish Transaction Failed Event
	 */
	public CompletableFuture<SendResult<String, Object>> publishTransactionFailed(Transaction transaction) {
		Map<String, Object> event = buildTransactionEvent(transaction, "TransactionFailed");
		return publishEvent("banking.transaction.failed", transaction.getTransactionReference(), event);
	}

	/**
	 * Generic method to publish any transaction event
	 */
	public CompletableFuture<SendResult<String, Object>> publishTransactionEvent(String topic, String key,
			Object event) {
		return publishEvent(topic, key, event);
	}

	/**
	 * Build standardized transaction event structure
	 */
	private Map<String, Object> buildTransactionEvent(Transaction txn, String eventType) {
		Map<String, Object> event = new HashMap<>();
		event.put("eventType", eventType);
		event.put("aggregateId", txn.getTransactionReference());
		event.put("timestamp", LocalDateTime.now().toString());

		Map<String, Object> payload = new HashMap<>();
		payload.put("transactionReference", txn.getTransactionReference());
		payload.put("userId", txn.getUserId());
		payload.put("sourceAccountId", txn.getSourceAccountId());
		payload.put("destinationAccountId", txn.getDestinationAccountId());
		payload.put("amount", txn.getAmount());
		payload.put("currency", txn.getCurrency());
		payload.put("type", txn.getType().name());
		payload.put("status", txn.getStatus().name());
		payload.put("description", txn.getDescription());

		if (txn.getFraudScore() != null) {
			payload.put("fraudScore", txn.getFraudScore());
			payload.put("fraudStatus", txn.getFraudStatus());
		}

		if (txn.getFailureReason() != null) {
			payload.put("failureReason", txn.getFailureReason());
		}

		event.put("payload", payload);
		return event;
	}

	/**
	 * Internal method to publish event with proper error handling
	 */
	private CompletableFuture<SendResult<String, Object>> publishEvent(String topic, String key, Object event) {

		CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

		future.whenComplete((result, ex) -> {
			if (ex == null) {
				log.info("Event published successfully: topic={}, key={}, partition={}, offset={}", topic, key,
						result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
			} else {
				log.error("Failed to publish event: topic={}, key={}, error={}", topic, key, ex.getMessage(), ex);
			}
		});

		return future;
	}
}
