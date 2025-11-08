package com.account_service.dto;

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
@AllArgsConstructor
@NoArgsConstructor
public class CreateBeneficiaryRequest {
	@NotNull(message = "User ID is required")
	private Long userId;

	private Long accountId;

	@NotBlank(message = "Beneficiary name is required")
	@Size(min = 2, max = 100)
	private String beneficiaryName;

	@NotBlank(message = "Account number is required")
	@Pattern(regexp = "^[0-9]{9,18}$", message = "Invalid account number")
	private String beneficiaryAccountNumber;

	@NotBlank(message = "IFSC code is required")
	@Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code")
	private String beneficiaryIfsc;

	@NotBlank(message = "Bank name is required")
	private String beneficiaryBank;

	@Size(max = 50)
	private String nickname;
}
