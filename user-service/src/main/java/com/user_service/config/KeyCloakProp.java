package com.user_service.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;

public class KeyCloakProp {

	@Value("${app.config.keycloak.server-url}")
	private String serverUrl;

	@Value("${app.config.keycloak.realm}")
	private String realm;

	@Value("${app.config.keycloak.client-id}")
	private String clientId;

	@Value("${app.config.keycloak.client-secret}")
	private String clientSecret;

	private static Keycloak keycloakInstance = null;
	
	/**
	 * if the instance is null then it will create a new instance 
	 * @return keyCloak instance
	 */
	public Keycloak getKeycloakInstance() {
		
		if(keycloakInstance == null) {
			keycloakInstance = KeycloakBuilder.builder()
					.serverUrl(serverUrl)
					.realm(realm)
					.clientId(clientId)
					.clientSecret(clientSecret)
					.grantType("client_credentials")
					.build();
		}
		return keycloakInstance;
	}
	
	public String getRealm() {
		return realm;
	}
}
