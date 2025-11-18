package com.loan_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.loan_service.enums.LoanStatus;
import com.loan_service.enums.LoanType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
	private Long id;
    private String loanNumber;
    private Long userId;
    private Long accountId;
    private LoanType loanType;
    private LoanStatus status;
    private BigDecimal principalAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal totalOutstanding;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Integer remainingTenureMonths;
    private BigDecimal emiAmount;
    private LocalDate nextEmiDate;
    private Integer paidEmis;
    private Integer totalEmis;
    private Integer missedEmis;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LocalDateTime createdAt;
}
