package com.account_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceHoldRequest {
	@NotNull(message = "Account ID is required")
	private Long accountId;

	@NotNull(message = "Amount is required")
	@Positive(message = "Amount must be positive")
	private BigDecimal amount;

	@NotBlank(message = "Reason is required")
	private String reason;

	private String transactionReference;

	private Integer expiryHours = 24; // Default 24 hours
}
