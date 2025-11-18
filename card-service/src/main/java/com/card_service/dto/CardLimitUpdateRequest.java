package com.card_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardLimitUpdateRequest {
	private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal perTransactionLimit;
}
