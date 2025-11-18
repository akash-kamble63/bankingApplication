package com.loan_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.loan_service.enums.ApplicationStatus;
import com.loan_service.enums.LoanType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {
	private Long id;
    private String applicationNumber;
    private LoanType loanType;
    private ApplicationStatus status;
    private BigDecimal requestedAmount;
    private Integer requestedTenureMonths;
    private Integer creditScore;
    private BigDecimal fraudScore;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
