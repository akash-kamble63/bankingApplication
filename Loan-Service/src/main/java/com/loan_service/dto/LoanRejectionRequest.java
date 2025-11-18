package com.loan_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class LoanRejectionRequest {
	@NotNull
	private Long applicationId;
	@NotBlank
	private String rejectionReason;
}
