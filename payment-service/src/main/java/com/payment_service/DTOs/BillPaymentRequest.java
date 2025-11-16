package com.payment_service.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
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
public class BillPaymentRequest {
	@NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0")
    private BigDecimal amount;
    
    @NotBlank(message = "Biller ID is required")
    private String billerId;
    
    @NotBlank(message = "Bill number is required")
    private String billNumber;
    
    @NotBlank(message = "Biller name is required")
    private String billerName;
    
    @Size(max = 500)
    private String description;
    
    private String idempotencyKey;
}
