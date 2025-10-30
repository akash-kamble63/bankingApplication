package com.user_service.service;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;

public interface KeycloakService {

	/**
	 * to create user 
	 * @param userRepresentation
	 * @return status code
	 */
	Integer createUser(UserRepresentation userRepresentation);
	
	
	/**
	 * return the list of users based on email
	 * @return the list of users
	 */
	List<UserRepresentation> readUserByEmail(String email);
	
}
