package com.payment_service.clients;


import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.payment_service.DTOs.PaymentSagaData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceClient {
	private final WebClient notificationServiceWebClient;

    public void sendPaymentNotification(PaymentSagaData data) {
        log.debug("Sending payment notification for: {}", data.getPaymentReference());
        
        notificationServiceWebClient.post()
            .uri("/api/v1/notifications/payment")
            .bodyValue(Map.of(
                "userId", data.getUserId(),
                "paymentReference", data.getPaymentReference(),
                "amount", data.getAmount(),
                "status", "COMPLETED"
            ))
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(); // Async - fire and forget
    }
}
