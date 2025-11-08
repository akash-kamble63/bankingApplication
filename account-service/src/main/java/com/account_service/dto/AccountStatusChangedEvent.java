package com.account_service.dto;

import com.account_service.enums.AccountStatus;
import com.account_service.patterns.DomainEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusChangedEvent extends DomainEvent {
    private String accountNumber;
    private AccountStatus previousStatus;
    private AccountStatus newStatus;
    private String reason;
}