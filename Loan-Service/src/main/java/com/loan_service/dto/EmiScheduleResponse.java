package com.loan_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.loan_service.enums.EmiStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiScheduleResponse {
	private Long id;
    private Integer emiNumber;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private BigDecimal outstandingPrincipal;
    private EmiStatus status;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private BigDecimal latePaymentCharges;
    private Integer daysOverdue;
}
