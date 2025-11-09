package com.transaction_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
	private String transactionReference;
    private Long userId;
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
}
