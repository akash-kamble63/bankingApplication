package com.account_service.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.account_service.enums.AccountStatus;
import com.account_service.enums.AccountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountFilterRequest {
	private Long userId;
    private String accountNumber;
    private List<AccountType> accountTypes;
    private List<AccountStatus> statuses;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private String currency;
    private Boolean isPrimary;
    
    // Pagination
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
	
}
