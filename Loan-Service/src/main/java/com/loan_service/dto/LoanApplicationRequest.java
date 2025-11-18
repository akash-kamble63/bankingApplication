package com.loan_service.dto;

import java.math.BigDecimal;

import com.loan_service.enums.LoanType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {
	@NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotNull(message = "Loan type is required")
    private LoanType loanType;
    
    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "10000.0", message = "Minimum loan amount is 10,000")
    @DecimalMax(value = "10000000.0", message = "Maximum loan amount is 1 crore")
    private BigDecimal requestedAmount;
    
    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    private Integer requestedTenureMonths;
    
    @NotBlank(message = "Loan purpose is required")
    @Size(max = 500)
    private String loanPurpose;
    
    @NotNull(message = "Annual income is required")
    @DecimalMin(value = "100000.0", message = "Minimum annual income is 1 lakh")
    private BigDecimal annualIncome;
    
    @NotBlank(message = "Employment type is required")
    private String employmentType;
    
    private String companyName;
    
    private BigDecimal monthlyObligations;
    
    private Boolean collateralOffered = false;
    
    private String collateralDetails;
}
