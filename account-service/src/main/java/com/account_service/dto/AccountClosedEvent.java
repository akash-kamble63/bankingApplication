package com.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.account_service.patterns.DomainEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountClosedEvent extends DomainEvent {
    private String accountNumber;
    private BigDecimal finalBalance;
    private String closureReason;
    private LocalDateTime closedAt;
}
