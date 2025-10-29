package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {

	private String firstName;

	private String lastName;

	private String gender;

	private String address;

	private String occupation;

	private String martialStatus;

	private String nationality;
}
