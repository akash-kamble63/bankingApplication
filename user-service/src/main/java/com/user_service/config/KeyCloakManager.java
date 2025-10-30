package com.user_service.config;

import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeyCloakManager {

	private final KeyCloakProp keyCloakProp;
	
	public RealmResource getKeyCloakInstanceWithRealm() {
		return keyCloakProp.getKeycloakInstance().realm(keyCloakProp.getRealm());
	}
}
