package com.account_service.dto;

import java.math.BigDecimal;

import com.account_service.enums.AccountHolderType;
import com.account_service.enums.AccountType;

import jakarta.validation.constraints.Email;
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
public class CreateAccountRequest {
	@NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String userEmail;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    @NotNull(message = "Holder type is required")
    private AccountHolderType holderType;
    
    @Positive(message = "Initial deposit must be positive")
    private BigDecimal initialDeposit = BigDecimal.ZERO;
    
    private String branchCode;
    
    private Boolean isPrimary = false;
    
    private String currency = "INR";
}
