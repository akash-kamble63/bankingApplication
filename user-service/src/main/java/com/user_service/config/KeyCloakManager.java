package com.user_service.config;

import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyCloakManager {

	private final KeyCloakProp keyCloakProp;
	
	 public RealmResource getKeyCloakInstanceWithRealm() {
	        try {
	            return keyCloakProp.getKeycloakInstance()
	                    .realm(keyCloakProp.getRealm());
	        } catch (Exception e) {
	            log.error("Error getting Keycloak realm resource: {}", e.getMessage(), e);
	            throw new RuntimeException("Failed to get Keycloak realm resource", e);
	        }
	    }
	 /**
	     * Get the realm name
	     * @return Realm name
	     */
	    public String getRealm() {
	        return keyCloakProp.getRealm();
	    }
}
