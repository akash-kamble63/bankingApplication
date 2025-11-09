package com.transaction_service.DTO;

import java.math.BigDecimal;

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
public class BillPaymentRequest {
	@NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Biller name is required")
    private String billerName;

    @NotBlank(message = "Bill category is required")
    private String billCategory;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String billNumber;
    private String dueDate;
}
