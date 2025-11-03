package com.user_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

	@Size(max = 10, message = "Gender must not exceed 10 characters")
	private String gender;

	@Size(max = 255, message = "Address must not exceed 255 characters")
	private String address;

	@Size(max = 100, message = "Occupation must not exceed 100 characters")
	private String occupation;

	@Size(max = 20, message = "Marital status must not exceed 20 characters")
	private String maritalStatus;

	@Size(max = 50, message = "Nationality must not exceed 50 characters")
	private String nationality;
}
