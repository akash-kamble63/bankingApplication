package com.account_service.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.account_service.model.Account;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishAccountCreated(Account account) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "AccountCreated");
            event.put("aggregateId", "ACCOUNT-" + account.getId());
            event.put("timestamp", LocalDateTime.now().toString());
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountId", account.getId());
            payload.put("accountNumber", account.getAccountNumber());
            payload.put("userId", account.getUserId());
            payload.put("userEmail", account.getUserEmail());
            payload.put("accountType", account.getAccountType().name());
            payload.put("status", account.getStatus().name());
            payload.put("currency", account.getCurrency());
            payload.put("balance", account.getBalance());
            
            event.put("payload", payload);
            
            publishEvent("banking.account.created", "ACCOUNT-" + account.getId(), event);
            log.info("Published AccountCreated event for account: {}", account.getAccountNumber());
            
        } catch (Exception e) {
            log.error("Failed to publish AccountCreated event: {}", e.getMessage(), e);
        }
    }

    public void publishBalanceUpdated(Account account, String transactionType, 
                                     java.math.BigDecimal amount, String reference) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BalanceUpdated");
            event.put("aggregateId", "ACCOUNT-" + account.getId());
            event.put("timestamp", LocalDateTime.now().toString());
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountId", account.getId());
            payload.put("accountNumber", account.getAccountNumber());
            payload.put("userId", account.getUserId());
            payload.put("transactionType", transactionType);
            payload.put("amount", amount);
            payload.put("newBalance", account.getBalance());
            payload.put("reference", reference);
            
            event.put("payload", payload);
            
            publishEvent("banking.balance.updated", "ACCOUNT-" + account.getId(), event);
            
        } catch (Exception e) {
            log.error("Failed to publish BalanceUpdated event: {}", e.getMessage(), e);
        }
    }

    private void publishEvent(String topic, String key, Map<String, Object> event) {
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Event sent to {} partition {} offset {}", 
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }
}
