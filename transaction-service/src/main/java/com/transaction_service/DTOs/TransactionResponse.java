package com.transaction_service.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
	 private Long id;
	    private String transactionReference;
	    private BigDecimal amount;
	    private BigDecimal feeAmount;
	    private String currency;
	    private TransactionStatus status;
	    private TransactionType type;
	    private String description;
	    private LocalDateTime createdAt;
	    private LocalDateTime completedAt;
}
