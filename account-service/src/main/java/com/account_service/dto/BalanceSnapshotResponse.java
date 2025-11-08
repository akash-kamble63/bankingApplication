package com.account_service.dto;
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
public class BalanceSnapshotResponse {
    private Long id;
    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private LocalDateTime snapshotDate;
    private String snapshotType;
}