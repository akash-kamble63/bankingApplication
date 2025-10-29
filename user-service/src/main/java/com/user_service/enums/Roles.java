package com.user_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Roles {

	CUSTOMER,
	ADMIN,
	ACCOUNTANT;
	
	
	@JsonCreator
	public static Roles fromString(String value) {
		return Roles.valueOf(value.toUpperCase());
		
	}
}
