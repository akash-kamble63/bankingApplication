package com.loan_service.dto;

import java.math.BigDecimal;

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
public class PrepaymentRequest {
	@NotNull
    @DecimalMin("1.0")
    private BigDecimal amount;
    private Boolean fullPayment = false;
}
