package com.transaction_service.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldResponse {
	private Long id;
	private String holdReference;
	private Long accountId;
	private BigDecimal amount;
	private String status;
	private String reason;
	private String transactionReference;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
}
