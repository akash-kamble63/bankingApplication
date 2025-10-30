package com.user_service.service.implementation;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.service.KeycloakService;

import lombok.RequiredArgsConstructor;

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
	
	

}
