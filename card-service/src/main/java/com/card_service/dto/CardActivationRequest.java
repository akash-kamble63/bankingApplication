package com.card_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardActivationRequest {
	@NotBlank(message = "Activation code is required")
	@Pattern(regexp = "^[0-9]{6}$", message = "Invalid activation code")
	private String activationCode;

	@NotBlank(message = "PIN is required")
	@Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
	private String pin;
}
