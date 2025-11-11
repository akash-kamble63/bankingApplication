package com.transaction_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.transaction_service.DTOs.FraudResultEvent;
import com.transaction_service.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FraudResultListener {
	private final TransactionService transactionService;

	@KafkaListener(topics = "banking.fraud.result", groupId = "transaction-service-group", concurrency = "3")
	public void handleFraudResult(@Payload FraudResultEvent event,
			@Header(KafkaHeaders.RECEIVED_PARTITION) int partition, Acknowledgment acknowledgment) {

		try {
			log.info("Received fraud result: txn={}, score={}", event.getTransactionReference(), event.getFraudScore());

			transactionService.updateFraudStatus(event.getTransactionReference(), event.getFraudScore(),
					event.getFraudStatus());

			// Manual acknowledgment (at-least-once processing)
			acknowledgment.acknowledge();

		} catch (Exception e) {
			log.error("Error processing fraud result: {}", e.getMessage(), e);
			// Don't acknowledge - will be retried
		}
	}
}
