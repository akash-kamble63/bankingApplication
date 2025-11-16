package com.payment_service.DTOs;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiPaymentRequest {
	@NotNull(message = "User ID is required")
    private Long userId;
    
    private Long accountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0")
    @DecimalMax(value = "100000.0")
    private BigDecimal amount;
    
    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+@[a-zA-Z]+$", message = "Invalid UPI ID")
    private String upiId;
    
    private Long recipientId;
    
    @Size(max = 100)
    private String recipientName;
    
    @Size(max = 500)
    private String description;
    
    private String idempotencyKey;
}
