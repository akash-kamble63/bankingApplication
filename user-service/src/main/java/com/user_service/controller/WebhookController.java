package com.user_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.service.UserVerificationWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {
	private final UserVerificationWebhookService webhookService;
	
	@PostMapping("/keycloak/user-events")
    public ResponseEntity<?> handleKeycloakEvent(@RequestBody Map<String, Object> event) {
        log.info("Received Keycloak event: {}", event);
        
        try {
            String eventType = (String) event.get("type");
            
            if ("VERIFY_EMAIL".equals(eventType)) {
                String userId = (String) event.get("userId");
                webhookService.handleEmailVerification(userId);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
