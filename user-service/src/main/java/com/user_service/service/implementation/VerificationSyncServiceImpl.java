package com.user_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.enums.UserStatus;
import com.user_service.event.KeycloakEventType;
import com.user_service.event.KeycloakWebhookEvent;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.VerificationSyncService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationSyncServiceImpl implements VerificationSyncService {
    
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final LockRegistry lockRegistry;  // FIX: Add distributed lock registry
    
    @PersistenceContext
    private EntityManager entityManager;  // FIX: Add EntityManager for pessimistic locking
    
    /**
     * Process Keycloak webhook events asynchronously
     * Uses separate thread pool to avoid blocking the webhook endpoint
     */
    @Override
    @Async("webhookExecutor")
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
     * FIX: Added distributed locking and pessimistic locking to prevent race conditions
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)  // FIX: Set appropriate isolation level
    public void syncUserVerificationStatus(String authId) {
        // FIX: Use distributed lock to prevent concurrent processing of same user
        Lock lock = lockRegistry.obtain("user-verification:" + authId);
        
        try {
            // FIX: Try to acquire lock with timeout
            if (!lock.tryLock()) {
                log.info("Another process is already syncing user {}, skipping", authId);
                return;
            }
            
            log.debug("Syncing verification status for user with authId: {}", authId);
            
            try {
                // Find user in database with pessimistic lock
                User user = userRepository.findByAuthId(authId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with authId: " + authId));
                
                // FIX: Apply pessimistic write lock
                entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
                
                // FIX: Double-check status after acquiring lock (double-checked locking pattern)
                if (user.getStatus() == UserStatus.ACTIVE && user.getEmailVerifiedAt() != null) {
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
                    
                    user.setStatus(UserStatus.ACTIVE);
                    user.setEmailVerifiedAt(LocalDateTime.now());
                    userRepository.saveAndFlush(user);  // FIX: Use saveAndFlush
                    
                    log.info("Successfully updated user {} status to ACTIVE", user.getEmail());
                } else {
                    log.debug("Email not yet verified in Keycloak for user: {}", user.getEmail());
                }
                
            } catch (ResourceNotFoundException e) {
                log.error("User not found: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error syncing verification status for authId {}: {}", authId, e.getMessage(), e);
                throw new RuntimeException("Failed to sync verification status", e);
            }
            
        } finally {
            // FIX: Always release the lock
            try {
                lock.unlock();
            } catch (Exception e) {
                log.error("Error releasing lock for authId {}: {}", authId, e.getMessage());
            }
        }
    }
    
    /**
     * Scheduled job to sync all pending users
     * Runs every 5 minutes as a fallback mechanism
     * FIX: Added locking to prevent concurrent execution
     */
    @Override
    @Scheduled(cron = "${app.sync.verification.cron:0 */5 * * * ?}")
    public void syncVerificationStatus() {
        // FIX: Use distributed lock to prevent multiple instances from running simultaneously
        Lock schedulerLock = lockRegistry.obtain("verification-sync-scheduler");
        
        try {
            // FIX: Try to acquire lock without waiting - if another instance is running, skip
            if (!schedulerLock.tryLock()) {
                log.info("Verification sync job already running in another instance, skipping");
                return;
            }
            
            log.info("=== Starting scheduled verification status sync job ===");
            
            executeSyncJob();
            
        } finally {
            try {
                schedulerLock.unlock();
            } catch (Exception e) {
                log.error("Error releasing scheduler lock: {}", e.getMessage());
            }
        }
    }
    
    /**
     * FIX: Extracted sync job logic to separate transactional method
     * This ensures proper transaction boundaries
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    protected void executeSyncJob() {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;
        
        try {
            // Find all users with PENDING status
            List<User> pendingUsers = userRepository.findByStatus(UserStatus.PENDING);
            
            if (pendingUsers.isEmpty()) {
                log.info("No pending users found for verification sync");
                return;
            }
            
            log.info("Found {} pending users to check for verification", pendingUsers.size());
            
            for (User user : pendingUsers) {
                try {
                    // FIX: Use distributed lock for each user to prevent concurrent updates
                    Lock userLock = lockRegistry.obtain("user-verification:" + user.getAuthId());
                    
                    try {
                        if (!userLock.tryLock()) {
                            log.debug("User {} is being processed by another thread, skipping", user.getEmail());
                            skippedCount++;
                            continue;
                        }
                        
                        // FIX: Reload user with pessimistic lock within this transaction
                        User lockedUser = userRepository.findById(user.getId())
                                .orElse(null);
                        
                        if (lockedUser == null) {
                            log.warn("User {} not found during sync", user.getEmail());
                            errorCount++;
                            continue;
                        }
                        
                        entityManager.lock(lockedUser, LockModeType.PESSIMISTIC_WRITE);
                        
                        // FIX: Double-check status after acquiring lock
                        if (lockedUser.getStatus() != UserStatus.PENDING) {
                            log.debug("User {} status changed to {}, skipping", 
                                    lockedUser.getEmail(), lockedUser.getStatus());
                            skippedCount++;
                            continue;
                        }
                        
                        // Get user from Keycloak
                        UserRepresentation keycloakUser = keycloakService.getUserById(lockedUser.getAuthId());
                        
                        if (keycloakUser == null) {
                            log.warn("User not found in Keycloak: email={}, authId={}", 
                                    lockedUser.getEmail(), lockedUser.getAuthId());
                            errorCount++;
                            continue;
                        }
                        
                        // Check if email is verified
                        if (Boolean.TRUE.equals(keycloakUser.isEmailVerified())) {
                            log.info("Updating user {} from PENDING to ACTIVE", lockedUser.getEmail());
                            
                            lockedUser.setStatus(UserStatus.ACTIVE);
                            lockedUser.setEmailVerifiedAt(LocalDateTime.now());
                            userRepository.saveAndFlush(lockedUser);  // FIX: Use saveAndFlush
                            
                            successCount++;
                        } else {
                            log.debug("User {} still not verified in Keycloak", lockedUser.getEmail());
                            skippedCount++;
                        }
                        
                    } finally {
                        try {
                            userLock.unlock();
                        } catch (Exception e) {
                            log.error("Error releasing lock for user {}: {}", 
                                    user.getEmail(), e.getMessage());
                        }
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