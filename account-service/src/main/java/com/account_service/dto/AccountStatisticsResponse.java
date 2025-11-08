package com.account_service.dto;
import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatisticsResponse {
	private Long totalAccounts;
    private Long activeAccounts;
    private Long inactiveAccounts;
    private Long frozenAccounts;
    private Long closedAccounts;
    private BigDecimal totalBalance;
    private BigDecimal totalAvailableBalance;
    private Map<String, Long> accountsByType;
    private Map<String, BigDecimal> balanceByType;
    private Map<String, Long> accountsByStatus;
}
