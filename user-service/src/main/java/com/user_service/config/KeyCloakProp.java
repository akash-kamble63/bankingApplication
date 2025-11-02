package com.user_service.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "app.config.keycloak")
public class KeyCloakProp {

	
	private String serverUrl;

	private String realm;

	private String clientId;

	private String clientSecret;

	private static Keycloak keycloakInstance;
	
	/**
	 * if the instance is null then it will create a new instance 
	 * @return keyCloak instance
	 */
public synchronized Keycloak getKeycloakInstance() {
        
        if (keycloakInstance == null) {
            log.info("Initializing Keycloak instance for realm: {}", realm);
            log.debug("Server URL: {}, Client ID: {}", serverUrl, clientId);
            
            try {
                keycloakInstance = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("client_credentials")
                        .build();
                
                // Test connection
                keycloakInstance.serverInfo().getInfo();
                log.info("Keycloak instance created and connected successfully");
                
            } catch (Exception e) {
                log.error("Failed to create Keycloak instance: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize Keycloak client", e);
            }
        }
        return keycloakInstance;
    }
    
    /**
     * Close Keycloak instance on application shutdown
     */
    public void closeKeycloakInstance() {
        if (keycloakInstance != null) {
            log.info("Closing Keycloak instance");
            keycloakInstance.close();
            keycloakInstance = null;
        }
    }
}
