package com.transaction_service.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
	private Long id;
    private String transactionReference;
    private String accountNumber;
    private Long userId;
    private String transactionType;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String fromAccount;
    private String toAccount;
    private String toAccountHolderName;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private String description;
    private String remarks;
    private String category;
    private String paymentMethod;
    private String bankReference;
    private BigDecimal fee;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private String channel;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
