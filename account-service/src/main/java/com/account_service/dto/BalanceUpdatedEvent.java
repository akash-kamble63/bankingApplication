package com.account_service.dto;

import java.math.BigDecimal;

import com.account_service.patterns.DomainEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdatedEvent extends DomainEvent {
    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal amount;
    private String operation; // CREDIT, DEBIT
    private String reason;
    private String transactionReference;
}