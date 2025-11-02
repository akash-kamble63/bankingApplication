package com.user_service.service;

import com.user_service.event.KeycloakWebhookEvent;

public interface VerificationSyncService {

	/**
     * Process Keycloak webhook events
     * @param event The Keycloak event
     */
    void processKeycloakEvent(KeycloakWebhookEvent event);
    
    /**
     * Scheduled job to sync email verification status from Keycloak to database
     * Runs as a fallback/reconciliation mechanism
     */
    void syncVerificationStatus();
    
    /**
     * Manually sync verification status for a specific user
     * @param authId Keycloak user ID
     */
    void syncUserVerificationStatus(String authId);
}
