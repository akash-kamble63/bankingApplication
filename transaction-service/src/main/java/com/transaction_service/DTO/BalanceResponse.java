package com.transaction_service.DTO;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal minimumBalance;
    private String currency;
}
