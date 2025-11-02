package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.ApiResponse;
import com.user_service.event.KeycloakWebhookEvent;
import com.user_service.service.VerificationSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {
private final VerificationSyncService verificationSyncService;
    
    /**
     * Webhook endpoint to receive Keycloak events
     * Must be publicly accessible (add to SecurityConfig)
     */
    @PostMapping("/keycloak/events")
    public ResponseEntity<ApiResponse<Void>> handleKeycloakEvent(
            @RequestBody KeycloakWebhookEvent event,
            @RequestHeader(value = "X-Keycloak-Secret", required = false) String secret) {
        
        log.info("Received Keycloak webhook event: type={}, userId={}", event.getType(), event.getUserId());
        
        // Validate webhook secret (security best practice)
        // TODO: Add webhook secret validation
        
        try {
            verificationSyncService.processKeycloakEvent(event);
            return ResponseEntity.ok(ApiResponse.success("Event processed successfully"));
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", e.getMessage(), e);
            return ResponseEntity
                    .internalServerError()
                    .body(ApiResponse.error("500", "Failed to process event"));
        }
    }
}
