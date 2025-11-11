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
public class FraudResultEvent {
	private String transactionReference;
	private BigDecimal fraudScore;
	private String fraudStatus; // APPROVED, REJECTED, REVIEW
	private String reason;
	private LocalDateTime processedAt;
}
