package com.banking.keycloak;


import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class BankingEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger log = Logger.getLogger(BankingEventListenerProviderFactory.class);
    
    private static final String PROVIDER_ID = "banking-event-listener";
    
    private String webhookUrl;
    private String webhookSecret;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new BankingEventListenerProvider(webhookUrl, webhookSecret);
    }

    @Override
    public void init(Config.Scope config) {
        // Read configuration from standalone.xml or environment variables
        webhookUrl = config.get("webhookUrl", "http://host.docker.internal:8090/api/webhooks/keycloak/events");
        webhookSecret = config.get("webhookSecret", "your-secret-key");
        
        log.infof("Banking Event Listener initialized with webhook URL: %s", webhookUrl);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    	// No post-initialization logic required for this SPI for now
    }

    @Override
    public void close() {
        // Cleanup
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}