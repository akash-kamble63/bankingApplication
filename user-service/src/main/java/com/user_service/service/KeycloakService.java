package com.user_service.service;

import org.keycloak.representations.idm.UserRepresentation;

public interface KeycloakService {

	/**
	 * to create user 
	 * @param userRepresentation
	 * @return status code
	 */
	Integer createUser(UserRepresentation userRepresentation);
	
	
	
}
