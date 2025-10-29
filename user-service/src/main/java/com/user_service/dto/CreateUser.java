package com.user_service.dto;

import com.user_service.enums.Roles;
import com.user_service.model.Profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUser {

	@NotBlank(message = "Username is required")
	private String username;
	@NotBlank(message = "Password cannot be empty")
	private String password;
	@NotNull(message = "Role is required")
	private Roles role;
	@Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
	private String email;
	@Valid
	private ProfileDto userProfile;
	
}
