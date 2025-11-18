package com.loan_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalRequest {
	@NotNull
    private Long applicationId;
    @NotNull
    private BigDecimal sanctionedAmount;
    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("50.0")
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String approvalNotes;
}
