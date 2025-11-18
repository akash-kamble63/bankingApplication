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
public class PinChangeRequest {
	@NotBlank(message = "Old PIN is required")
	@Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
	private String oldPin;

	@NotBlank(message = "New PIN is required")
	@Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
	private String newPin;
}
