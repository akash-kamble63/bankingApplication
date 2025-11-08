package com.account_service.dto;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountSummary {
	private Long userId;
    private String userEmail;
    private Integer totalAccounts;
    private Integer activeAccounts;
    private Integer inactiveAccounts;
    private Integer frozenAccounts;
    private BigDecimal totalBalance;
    private BigDecimal totalAvailableBalance;
    private List<AccountSummaryResponse> accounts;
    private AccountSummaryResponse primaryAccount;
}
