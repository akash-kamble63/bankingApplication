package com.account_service.patterns;

import java.math.BigDecimal;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent extends DomainEvent {
	private String accountNumber;
    private Long userId;
    private String userEmail;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal initialBalance;
    private String currency;
    private String branchCode;
}
