package com.account_service.kafka;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.account_service.dto.CreateAccountRequest;
import com.account_service.enums.AccountHolderType;
import com.account_service.enums.AccountType;
import com.account_service.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {
	private final AccountService accountService;
	private final ObjectMapper objectMapper;

	/**
	 * Listen to User Registration events and create default account
	 */
	@KafkaListener(topics = "banking.user.events", groupId = "account-service-user-group", containerFactory = "kafkaListenerContainerFactory")
	public void consumeUserEvent(String message, Acknowledgment acknowledgment) {
		try {
			log.info("Received user event: {}", message);

			Map<String, Object> event = objectMapper.readValue(message, Map.class);
			String eventType = (String) event.get("eventType");
			Map<String, Object> payload = (Map<String, Object>) event.get("payload");

			if ("UserRegistered".equals(eventType)) {
				handleUserRegistered(payload);
			}

			acknowledgment.acknowledge();

		} catch (Exception e) {
			log.error("Failed to process user event: {}", e.getMessage(), e);
			// Don't acknowledge - will be retried
		}
	}

	private void handleUserRegistered(Map<String, Object> payload) {
		try {
			Long userId = ((Number) payload.get("userId")).longValue();
			String email = (String) payload.get("email");

			log.info("Creating default account for user: {}", userId);

			// Create default savings account
			CreateAccountRequest request = CreateAccountRequest.builder().userId(userId).userEmail(email)
					.accountType(AccountType.SAVINGS).holderType(AccountHolderType.INDIVIDUAL).currency("INR")
					.isPrimary(true).initialDeposit(BigDecimal.ZERO).build();

			accountService.createAccount(request, "SYSTEM");

			log.info("Successfully created default account for user: {}", userId);

		} catch (Exception e) {
			log.error("Failed to create default account for user: {}", e.getMessage(), e);
			throw e; // Propagate for retry
		}
	}
}
