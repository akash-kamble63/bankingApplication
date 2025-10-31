package com.user_service.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.config.keycloak")
public class KeyCloakProp {

	
	private String serverUrl;

	private String realm;

	private String clientId;

	private String clientSecret;

	private static Keycloak keycloakInstance = null;
	
	/**
	 * if the instance is null then it will create a new instance 
	 * @return keyCloak instance
	 */
	public Keycloak getKeycloakInstance() {
        if (keycloakInstance == null) {
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
