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
	
	
	void sendVerificationEmail(String userId);
	
	UserRepresentation getUserById(String userId);
	
    void updateKeycloakUser(String userId, UserRepresentation userRepresentation);
    
    
    void changePassword(String userId, String newPassword);
    void sendPasswordResetEmail(String userId);
    boolean verifyPassword(String userId, String password);
	
	
}
