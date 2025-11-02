package com.user_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.user_service.enums.Status;
import com.user_service.event.KeycloakEventType;
import com.user_service.event.KeycloakWebhookEvent;
import com.user_service.exception.ResourceNotFound;
import com.user_service.model.Users;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.VerificationSyncService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
@Async("webhookExecutor")
@Transactional
public class VerificationSyncServiceImpl implements VerificationSyncService{
	private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    
    /**
     * Process Keycloak webhook events asynchronously
     * Uses separate thread pool to avoid blocking the webhook endpoint
     */
    @Override
    public void processKeycloakEvent(KeycloakWebhookEvent event) {
        log.info("Processing Keycloak event asynchronously: type={}, userId={}", 
                event.getType(), event.getUserId());
        
        try {
            // Check if this is a verification event
            if (isVerificationEvent(event.getType())) {
                String userId = event.getUserId();
                
                if (userId == null || userId.isEmpty()) {
                    log.warn("Received verification event without userId");
                    return;
                }
                
                syncUserVerificationStatus(userId);
                log.info("Successfully processed verification event for user: {}", userId);
            } else {
                log.debug("Ignoring non-verification event: {}", event.getType());
            }
            
        } catch (Exception e) {
            log.error("Error processing Keycloak event: {}", e.getMessage(), e);
            // Don't throw exception - webhook should return 200 to prevent retries
        }
    }
    
    /**
     * Sync verification status for a specific user
     */
    @Override
    @Transactional
    public void syncUserVerificationStatus(String authId) {
        log.debug("Syncing verification status for user with authId: {}", authId);
        
        try {
            // Find user in database
            Users user = userRepository.findByAuthId(authId)
                    .orElseThrow(() -> new ResourceNotFound("User not found with authId: " + authId));
            
            // Skip if already active
            if (user.getStatus() == Status.ACTIVE && user.getEmailVerifiedAt() != null) {
                log.debug("User {} already active and verified, skipping sync", user.getEmail());
                return;
            }
            
            // Get user from Keycloak
            UserRepresentation keycloakUser = keycloakService.getUserById(authId);
            
            if (keycloakUser == null) {
                log.warn("User not found in Keycloak with authId: {}", authId);
                return;
            }
            
            // Check if email is verified in Keycloak
            if (Boolean.TRUE.equals(keycloakUser.isEmailVerified())) {
                log.info("Email verified in Keycloak for user: {}, updating database", user.getEmail());
                
                user.setStatus(Status.ACTIVE);
                user.setEmailVerifiedAt(LocalDateTime.now());
                userRepository.save(user);
                
                log.info("Successfully updated user {} status to ACTIVE", user.getEmail());
            } else {
                log.debug("Email not yet verified in Keycloak for user: {}", user.getEmail());
            }
            
        } catch (ResourceNotFound e) {
            log.error("User not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error syncing verification status for authId {}: {}", authId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync verification status", e);
        }
    }
    
    /**
     * Scheduled job to sync all pending users
     * Runs every 5 minutes as a fallback mechanism
     * Cron: "second minute hour day month weekday"
     */
    @Override
    @Scheduled(cron = "${app.sync.verification.cron:0 */5 * * * ?}")
    @Transactional
    public void syncVerificationStatus() {
        log.info("=== Starting scheduled verification status sync job ===");
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;
        
        try {
            // Find all users with PENDING status
            List<Users> pendingUsers = userRepository.findByStatus(Status.PENDING);
            
            if (pendingUsers.isEmpty()) {
                log.info("No pending users found for verification sync");
                return;
            }
            
            log.info("Found {} pending users to check for verification", pendingUsers.size());
            
            for (Users user : pendingUsers) {
                try {
                    // Get user from Keycloak
                    UserRepresentation keycloakUser = keycloakService.getUserById(user.getAuthId());
                    
                    if (keycloakUser == null) {
                        log.warn("User not found in Keycloak: email={}, authId={}", 
                                user.getEmail(), user.getAuthId());
                        errorCount++;
                        continue;
                    }
                    
                    // Check if email is verified
                    if (Boolean.TRUE.equals(keycloakUser.isEmailVerified())) {
                        log.info("Updating user {} from PENDING to ACTIVE", user.getEmail());
                        
                        user.setStatus(Status.ACTIVE);
                        user.setEmailVerifiedAt(LocalDateTime.now());
                        userRepository.save(user);
                        
                        successCount++;
                    } else {
                        log.debug("User {} still not verified in Keycloak", user.getEmail());
                        skippedCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Error syncing user {}: {}", user.getEmail(), e.getMessage());
                    errorCount++;
                }
            }
            
        } catch (Exception e) {
            log.error("Critical error in verification sync job: {}", e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Verification sync job completed in {}ms ===", duration);
            log.info("Results - Success: {}, Errors: {}, Skipped: {}", 
                    successCount, errorCount, skippedCount);
        }
    }
    
    /**
     * Check if the event type is related to email verification
     */
    private boolean isVerificationEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        
        try {
            KeycloakEventType type = KeycloakEventType.valueOf(eventType);
            return type == KeycloakEventType.VERIFY_EMAIL || 
                   type == KeycloakEventType.SEND_VERIFY_EMAIL;
        } catch (IllegalArgumentException e) {
            log.debug("Unknown event type: {}", eventType);
            return false;
        }
    }
}
