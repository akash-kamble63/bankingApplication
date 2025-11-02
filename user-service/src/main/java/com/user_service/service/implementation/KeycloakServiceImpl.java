package com.user_service.service.implementation;

import java.util.Collections;
import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.exception.KeycloakOperationException;
import com.user_service.service.KeycloakService;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {
	
private final KeyCloakManager keyCloakManager;
    
    @Override
    public Integer createUser(UserRepresentation userRepresentation) {
        log.info("Creating user in Keycloak: {}", userRepresentation.getEmail());
        
        try {
            Response response = keyCloakManager.getKeyCloakInstanceWithRealm()
                    .users()
                    .create(userRepresentation);
            
            int status = response.getStatus();
            response.close();
            
            log.info("Keycloak user creation response: {}", status);
            return status;
            
        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", e.getMessage(), e);
            throw new KeycloakOperationException("Failed to create user in Keycloak", e);
        }
    }
    
    @Override
    public List<UserRepresentation> readUserByEmail(String email) {
        log.debug("Searching for user by email in Keycloak: {}", email);
        
        try {
            List<UserRepresentation> users = keyCloakManager.getKeyCloakInstanceWithRealm()
                    .users()
                    .search(email, true); // exact match
            
            log.debug("Found {} users with email: {}", users.size(), email);
            return users;
            
        } catch (Exception e) {
            log.error("Error searching user by email in Keycloak: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void sendVerificationEmail(String userId) {
        log.info("Sending verification email for user ID: {}", userId);
        
        try {
            keyCloakManager.getKeyCloakInstanceWithRealm()
                    .users()
                    .get(userId)
                    .sendVerifyEmail();
            
            log.info("Verification email sent successfully for user ID: {}", userId);
            
        } catch (NotFoundException e) {
            log.error("User not found in Keycloak with ID: {}", userId);
            throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
        } catch (Exception e) {
            log.error("Error sending verification email for user ID {}: {}", userId, e.getMessage(), e);
            throw new KeycloakOperationException("Failed to send verification email", e);
        }
    }
    
    @Override
    public UserRepresentation getUserById(String userId) {
        log.debug("Fetching user from Keycloak with ID: {}", userId);
        
        try {
            UserRepresentation user = keyCloakManager.getKeyCloakInstanceWithRealm()
                    .users()
                    .get(userId)
                    .toRepresentation();
            
            log.debug("Successfully fetched user from Keycloak: {}", user.getEmail());
            return user;
            
        } catch (NotFoundException e) {
            log.warn("User not found in Keycloak with ID: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching user from Keycloak with ID {}: {}", userId, e.getMessage(), e);
            throw new KeycloakOperationException("Failed to fetch user from Keycloak", e);
        }
    }
	

}
