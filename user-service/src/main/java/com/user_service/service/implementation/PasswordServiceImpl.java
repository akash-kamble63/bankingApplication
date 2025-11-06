package com.user_service.service.implementation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.annotation.Auditable;
import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.ForgotPasswordRequest;
import com.user_service.dto.ResetPasswordRequest;
import com.user_service.enums.AuditAction;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.model.PasswordResetTokenRedis;
import com.user_service.model.User;
import com.user_service.repository.PasswordResetTokenRedisRepository;
import com.user_service.repository.UserRepository;
import com.user_service.service.AuditService;
import com.user_service.service.KeycloakService;
import com.user_service.service.PasswordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {
	private final UserRepository userRepository;
    private final PasswordResetTokenRedisRepository tokenRedisRepository;
    private final KeycloakService keycloakService;
    private final AuditService auditService;
    
    @Value("${app.password.reset.token-expiry-hours:24}")
    private int tokenExpiryHours;
    
    @Value("${app.password.reset.max-attempts:3}")
    private int maxResetAttempts;
    
    @Override
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_CHANGED, entityType = "PasswordResetToken")
    public ApiResponse<Void> changePassword(String email, ChangePasswordRequest request) {
        log.info("Processing password change request for user: {}", email);
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error("400", "New password and confirm password do not match");
        }
        
        // Check if new password is same as current
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            return ApiResponse.error("400", "New password must be different from current password");
        }
        
        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify current password
        boolean isCurrentPasswordValid = keycloakService.verifyPassword(user.getAuthId(), request.getCurrentPassword());
        if (!isCurrentPasswordValid) {
            return ApiResponse.error("401", "Current password is incorrect");
        }
        
        // Change password in Keycloak
        keycloakService.changePassword(user.getAuthId(), request.getNewPassword());
        
        log.info("Password changed successfully for user: {}", email);
        
        return ApiResponse.success("Password changed successfully");
    }
    
    @Override
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_RESET_COMPLETED, entityType = "PasswordResetToken")
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        // Check if user already has an active reset token
        var existingToken = tokenRedisRepository.findByUserId(user.getId());
        if (existingToken.isPresent()) {
            PasswordResetTokenRedis token = existingToken.get();
            
            // Check if too many attempts
            if (token.getAttempts() >= maxResetAttempts) {
                Long remainingTTL = tokenRedisRepository.getTTL(token.getToken());
                return ApiResponse.error("429", 
                    String.format("Too many reset attempts. Please try again in %d minutes", 
                        remainingTTL != null ? remainingTTL / 60 : 0));
            }
            
            // Increment attempts
            token.setAttempts(token.getAttempts() + 1);
            Long ttl = tokenRedisRepository.getTTL(token.getToken());
            if (ttl != null && ttl > 0) {
                tokenRedisRepository.save(token, Duration.ofSeconds(ttl));
            }
            
            log.info("Password reset link resent for user: {}. Attempt: {}/{}", 
                request.getEmail(), token.getAttempts(), maxResetAttempts);
            return ApiResponse.success("Password reset link has been sent to your email");
        }
        
        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusHours(tokenExpiryHours);
        
        PasswordResetTokenRedis resetToken = PasswordResetTokenRedis.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(now)
                .expiryDate(expiryDate)
                .used(false)
                .attempts(1)
                .build();
        
        // Save to Redis with TTL
        Duration ttl = Duration.ofHours(tokenExpiryHours);
        tokenRedisRepository.save(resetToken, ttl);
        
        // Generate reset link
        String resetLink = generateResetLink(token);
        log.info("Password reset token generated for user: {}. Token expires in {} hours", 
            request.getEmail(), tokenExpiryHours);
        log.info("Password reset link: {}", resetLink);
        
        // TODO: Send email with reset link
        // emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        
        // Optional: Also send via Keycloak
        try {
            keycloakService.sendPasswordResetEmail(user.getAuthId());
        } catch (Exception e) {
            log.warn("Failed to send Keycloak reset email: {}", e.getMessage());
        }
        
        return ApiResponse.success("Password reset link has been sent to your email");
    }
    
    @Override
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_RESET_COMPLETED, entityType = "PasswordResetToken")
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error("400", "New password and confirm password do not match");
        }
        
        // Find token in Redis
        PasswordResetTokenRedis resetToken = tokenRedisRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));
        
        // Check if token is already used
        if (resetToken.isUsed()) {
            return ApiResponse.error("400", "This reset token has already been used");
        }
        
        // Check if token is expired (Redis should auto-delete, but double-check)
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            tokenRedisRepository.delete(request.getToken());
            return ApiResponse.error("400", "Reset token has expired");
        }
        
        // Find user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Change password in Keycloak
        try {
            keycloakService.changePassword(user.getAuthId(), request.getNewPassword());
        } catch (Exception e) {
            log.error("Failed to change password in Keycloak: {}", e.getMessage(), e);
            return ApiResponse.error("500", "Failed to reset password. Please try again");
        }
        
        // Mark token as used (keep in Redis until TTL expires for audit)
        tokenRedisRepository.markAsUsed(request.getToken());
        
        log.info("Password reset successfully for user: {}", user.getEmail());
       
        return ApiResponse.success("Password has been reset successfully. You can now login with your new password");
    }
    
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Void> validateResetToken(String token) {
        log.debug("Validating reset token");
        
        // Check if token exists in Redis
        if (!tokenRedisRepository.exists(token)) {
            return ApiResponse.error("400", "Invalid or expired reset token");
        }
        
        PasswordResetTokenRedis resetToken = tokenRedisRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));
        
        // Check if already used
        if (resetToken.isUsed()) {
            return ApiResponse.error("400", "This reset token has already been used");
        }
        
        // Check expiry
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            tokenRedisRepository.delete(token);
            return ApiResponse.error("400", "Reset token has expired");
        }
        
        // Get remaining time
        Long remainingSeconds = tokenRedisRepository.getTTL(token);
        String message = remainingSeconds != null 
            ? String.format("Token is valid. Expires in %d minutes", remainingSeconds / 60)
            : "Token is valid";
        
        return ApiResponse.success(message);
    }
    
    /**
     * Generate password reset link
     */
    private String generateResetLink(String token) {
        // In production, use your frontend URL
        String frontendUrl = "http://localhost:3000"; // or get from config
        return String.format("%s/reset-password?token=%s", frontendUrl, token);
    }
    
    /**
     * Get active reset token for user (for monitoring/debugging)
     */
    public ApiResponse<String> getActiveResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        var tokenOpt = tokenRedisRepository.findByUserId(user.getId());
        if (tokenOpt.isEmpty()) {
            return ApiResponse.error("404", "No active reset token found");
        }
        
        PasswordResetTokenRedis token = tokenOpt.get();
        Long remainingSeconds = tokenRedisRepository.getTTL(token.getToken());
        
        String info = String.format(
            "Token: %s, Expires in: %d minutes, Attempts: %d/%d, Used: %s",
            token.getToken(),
            remainingSeconds != null ? remainingSeconds / 60 : 0,
            token.getAttempts(),
            maxResetAttempts,
            token.isUsed()
        );
        
        
        return ApiResponse.success(token.getToken(), info);
    }
    
    /**
     * Revoke/cancel active reset token for user
     */
    @Transactional
    public ApiResponse<Void> revokeResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        tokenRedisRepository.deleteByUserId(user.getId());
        log.info("Revoked password reset token for user: {}", email);
        
        return ApiResponse.success("Password reset token has been revoked");
    }
}
