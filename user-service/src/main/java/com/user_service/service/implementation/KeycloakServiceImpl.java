package com.user_service.service.implementation;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.service.KeycloakService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {
	
	private final KeyCloakManager keyCloakManager;
	
	/**
	 * Creates a new user in the KeyCloak system
	 * @return the status code indicating creation is success or failure 
	 */

	@Override
	public Integer createUser(UserRepresentation userRepresentation) {
		return keyCloakManager.getKeyCloakInstanceWithRealm().users().create(userRepresentation).getStatus();
	}

	
	/**
	 * get the list of users using email
	 * @return the list of users
	 */
	@Override
	public List<UserRepresentation> readUserByEmail(String email) {
		
		return keyCloakManager.getKeyCloakInstanceWithRealm().users().search(email);
	}
	
	@Override
	public void sendVerificationEmail(String userId) {
	    keyCloakManager.getKeyCloakInstanceWithRealm()
	        .users()
	        .get(userId)
	        .sendVerifyEmail();
	}
	
	@Override
    public UserRepresentation getUserById(String userId) {  // ✅ Add this method
        try {
            return keyCloakManager.getKeyCloakInstanceWithRealm()
                .users()
                .get(userId)
                .toRepresentation();
        } catch (Exception e) {
            log.error("Error fetching user from Keycloak with ID {}: {}", userId, e.getMessage());
            return null;
        }
    }
	

}
