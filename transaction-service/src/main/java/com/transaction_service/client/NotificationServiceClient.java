package com.transaction_service.client;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.transaction_service.DTOs.TransferSagaData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceClient {
	private final WebClient webClient;

	public void sendTransactionNotification(TransferSagaData data) {
		try {
			webClient.post().uri("/api/v1/notifications/transaction")
					.bodyValue(Map.of("userId", data.getUserId(), "transactionReference",
							data.getTransactionReference(), "amount", data.getAmount(), "type", "TRANSFER_COMPLETED"))
					.retrieve().bodyToMono(Void.class)
					.subscribe(result -> log.info("Notification sent for: {}", data.getTransactionReference()),
							error -> log.error("Failed to send notification: {}", error.getMessage()));

		} catch (Exception e) {
			log.error("Notification failed: {}", e.getMessage());
		}
	}
}
