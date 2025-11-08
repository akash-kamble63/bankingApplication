package com.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.account_service.enums.HoldStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountHoldResponse {
	private Long id;
	private String holdReference;
	private Long accountId;
	private BigDecimal amount;
	private HoldStatus status;
	private String reason;
	private String transactionReference;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private LocalDateTime releasedAt;
}
