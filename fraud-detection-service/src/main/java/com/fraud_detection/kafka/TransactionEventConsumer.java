package com.fraud_detection.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fraud_detection.dto.FraudCheckRequestDTO;
import com.fraud_detection.dto.FraudCheckResponseDTO;
import com.fraud_detection.dto.TransactionEventDTO;
import com.fraud_detection.service.FraudDetectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {
	private final FraudDetectionService fraudDetectionService;
	private final FraudEventProducer fraudEventProducer;

	@KafkaListener(topics = "${kafka.topic.transaction-created:transaction-created}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
	public void consumeTransactionCreated(@Payload TransactionEventDTO transaction,
			@Header(KafkaHeaders.RECEIVED_KEY) String key, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
			@Header(KafkaHeaders.OFFSET) long offset) {

		log.info("Consumed transaction event: {} from partition: {} with offset: {}", transaction.getTransactionId(),
				partition, offset);

		try {
			// Convert to fraud check request
			FraudCheckRequestDTO request = FraudCheckRequestDTO.builder().transactionId(transaction.getTransactionId())
					.accountId(transaction.getAccountId()).userId(transaction.getUserId())
					.amount(transaction.getAmount()).currency(transaction.getCurrency())
					.transactionType(transaction.getTransactionType()).merchantName(transaction.getMerchantName())
					.merchantCategory(transaction.getMerchantCategory())
					.locationCountry(transaction.getLocationCountry()).locationCity(transaction.getLocationCity())
					.latitude(transaction.getLatitude()).longitude(transaction.getLongitude())
					.deviceId(transaction.getDeviceId()).ipAddress(transaction.getIpAddress()).build();

			// Perform fraud check
			FraudCheckResponseDTO response = fraudDetectionService.checkFraud(request);

			// Publish fraud check result
			fraudEventProducer.publishFraudCheckCompleted(response);

			log.info("Fraud check completed for transaction: {} with status: {}", transaction.getTransactionId(),
					response.getStatus());

		} catch (Exception e) {
			log.error("Error processing transaction event: {}", transaction.getTransactionId(), e);
			// In production, you might want to send to a DLQ (Dead Letter Queue)
		}
	}

	@KafkaListener(topics = "${kafka.topic.payment-initiated:payment-initiated}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
	public void consumePaymentInitiated(@Payload TransactionEventDTO payment,
			@Header(KafkaHeaders.RECEIVED_KEY) String key) {

		log.info("Consumed payment event: {}", payment.getTransactionId());

		try {
			FraudCheckRequestDTO request = FraudCheckRequestDTO.builder().transactionId(payment.getTransactionId())
					.accountId(payment.getAccountId()).userId(payment.getUserId()).amount(payment.getAmount())
					.currency(payment.getCurrency()).transactionType("PAYMENT").merchantName(payment.getMerchantName())
					.merchantCategory(payment.getMerchantCategory()).locationCountry(payment.getLocationCountry())
					.locationCity(payment.getLocationCity()).latitude(payment.getLatitude())
					.longitude(payment.getLongitude()).deviceId(payment.getDeviceId()).ipAddress(payment.getIpAddress())
					.build();

			FraudCheckResponseDTO response = fraudDetectionService.checkFraud(request);
			fraudEventProducer.publishFraudCheckCompleted(response);

		} catch (Exception e) {
			log.error("Error processing payment event: {}", payment.getTransactionId(), e);
		}
	}
}
