package com.transaction_service.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {
	@NotBlank(message = "From account is required")
	private String fromAccount;

	@NotBlank(message = "To account is required")
	private String toAccount;

	@NotNull(message = "Amount is required")
	@Positive(message = "Amount must be positive")
	private BigDecimal amount;

	@Size(max = 500)
	private String description;

	private String paymentMethod; // UPI, NEFT, RTGS, IMPS
	private String upiId;
	private Boolean isBeneficiary;
}
