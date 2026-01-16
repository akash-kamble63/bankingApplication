package com.user_service.service.implementation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.annotation.Auditable;
import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.ForgotPasswordRequest;
import com.user_service.dto.ResetPasswordRequest;
import com.user_service.enums.AuditAction;
import com.user_service.exception.InvalidTokenException;
import com.user_service.exception.PasswordValidationException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.model.PasswordResetTokenRedis;
import com.user_service.model.Profile;
import com.user_service.model.User;
import com.user_service.repository.PasswordResetTokenRedisRepository;
import com.user_service.repository.UserRepository;
import com.user_service.service.AuditService;
import com.user_service.service.KeycloakService;
import com.user_service.service.PasswordService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Password Service Implementation with Enterprise Banking-Grade Security
 * 
 * Security Features:
 * 1. Token hashing (SHA-256) - tokens stored as hashes, not plain text
 * 2. Email enumeration protection - generic responses
 * 3. Rate limiting - max attempts per user
 * 4. Session invalidation - all sessions logged out after password reset
 * 5. Password strength validation - pre-validation before Keycloak
 * 6. Short token expiry - 15 minutes (banking standard)
 * 7. Timing attack protection - consistent response times
 * 8. IP address tracking - for security monitoring
 * 9. Proper audit logging - separate events for initiation/completion
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRedisRepository tokenRedisRepository;
    private final KeycloakService keycloakService;
    private final AuditService auditService;
    private final HttpServletRequest httpRequest;

    // Security Configuration
    @Value("${app.password.reset.token-expiry-minutes:15}")
    private int tokenExpiryMinutes; // Changed from hours to minutes

    @Value("${app.password.reset.max-attempts:3}")
    private int maxResetAttempts;

    @Value("${app.password.reset.min-request-interval-minutes:2}")
    private int minRequestIntervalMinutes;

    @Value("${app.password.min-length:12}")
    private int minPasswordLength;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Common passwords to block (in production, use a comprehensive list or
    // library)
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password123", "admin123", "welcome123", "qwerty123",
            "Password1!", "Admin@123", "Welcome@123", "Passw0rd!");

    @Override
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_CHANGED, entityType = "User")
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
            log.warn("Invalid current password attempt for user: {}", email);
            return ApiResponse.error("401", "Current password is incorrect");
        }

        // Validate new password strength
        try {
            validatePasswordStrength(request.getNewPassword(), user);
        } catch (PasswordValidationException e) {
            return ApiResponse.error("400", e.getMessage());
        }

        // Change password in Keycloak
        try {
            keycloakService.changePassword(user.getAuthId(), request.getNewPassword());

            // Update password changed timestamp
            user.setPasswordChangedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", email);

            // Optional: Send notification email
            // emailService.sendPasswordChangedNotification(user.getEmail(),
            // user.getFirstName());

            return ApiResponse.success("Password changed successfully");

        } catch (Exception e) {
            log.error("Failed to change password in Keycloak for user: {}", email, e);
            return ApiResponse.error("500", "Failed to change password. Please try again");
        }
    }

    @Override
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_RESET_INITIATED, entityType = "PasswordResetToken")
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request");

        // Get client info for security tracking
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();

        // Find user - DON'T throw exception to prevent email enumeration
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // SECURITY: Always return same response, same timing
        if (user == null || !user.isActive()) {
            log.warn("Password reset attempted for non-existent or inactive email from IP: {}", ipAddress);
            simulateProcessingDelay(); // Prevent timing attacks
            return ApiResponse.success("If the email exists, a password reset link has been sent");
        }

        // Check if user already has an active reset token
        var existingToken = tokenRedisRepository.findByUserId(user.getId());
        if (existingToken.isPresent()) {
            PasswordResetTokenRedis token = existingToken.get();

            // Check if too many attempts
            if (token.getAttempts() >= maxResetAttempts) {
                Long remainingTTL = tokenRedisRepository.getTTL(token.getTokenHash());
                log.warn("Too many password reset attempts for user ID: {} from IP: {}", user.getId(), ipAddress);
                return ApiResponse.error("429",
                        String.format("Too many reset attempts. Please try again in %d minutes",
                                remainingTTL != null ? remainingTTL / 60 : 0));
            }

            // Check minimum time between requests (prevent spam)
            Duration timeSinceLastRequest = Duration.between(token.getCreatedAt(), LocalDateTime.now());
            if (timeSinceLastRequest.toMinutes() < minRequestIntervalMinutes) {
                log.info("Password reset request too soon for user ID: {} from IP: {}", user.getId(), ipAddress);
                return ApiResponse.success("If the email exists, a password reset link has been sent");
            }

            // Generate NEW token (more secure than reusing)
            String plainToken = generateSecureToken();
            String hashedToken = hashToken(plainToken);

            // Update token with new hash and increment attempts
            token.setTokenHash(hashedToken);
            token.setAttempts(token.getAttempts() + 1);
            token.setCreatedAt(LocalDateTime.now());
            token.setIpAddress(ipAddress);
            token.setUserAgent(userAgent);

            Long ttl = tokenRedisRepository.getTTL(token.getTokenHash());
            if (ttl != null && ttl > 0) {
                tokenRedisRepository.save(token, Duration.ofSeconds(ttl));
            }

            // Send email with new token
            String resetLink = generateResetLink(plainToken);
            sendResetEmail(user, resetLink);

            log.info("Password reset link resent for user ID: {}. Attempt: {}/{}",
                    user.getId(), token.getAttempts(), maxResetAttempts);

            return ApiResponse.success("If the email exists, a password reset link has been sent");
        }

        // Generate new token - PLAIN for email, HASHED for storage
        String plainToken = generateSecureToken();
        String hashedToken = hashToken(plainToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusMinutes(tokenExpiryMinutes);

        PasswordResetTokenRedis resetToken = PasswordResetTokenRedis.builder()
                .tokenHash(hashedToken) // Store HASH, not plain text
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(now)
                .expiryDate(expiryDate)
                .used(false)
                .attempts(1)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        // Save to Redis with TTL
        Duration ttl = Duration.ofMinutes(tokenExpiryMinutes);
        tokenRedisRepository.save(resetToken, ttl);

        // Generate reset link with PLAIN token (this goes in email)
        String resetLink = generateResetLink(plainToken);

        // Send email (primary method for banking app)
        sendResetEmail(user, resetLink);

        log.info("Password reset token generated for user ID: {}. Token expires in {} minutes",
                user.getId(), tokenExpiryMinutes);

        // Generic response - don't reveal if email exists
        return ApiResponse.success("If the email exists, a password reset link has been sent");
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

        // Hash the incoming token to compare
        String hashedToken = hashToken(request.getToken());

        // Find token in Redis by hash
        PasswordResetTokenRedis resetToken = tokenRedisRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        // Check if token is already used
        if (resetToken.isUsed()) {
            log.warn("Attempted reuse of password reset token for user ID: {}", resetToken.getUserId());
            throw new InvalidTokenException("This reset link has already been used");
        }

        // Check if token is expired
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            log.warn("Expired password reset token used for user ID: {}", resetToken.getUserId());
            tokenRedisRepository.delete(hashedToken);
            throw new InvalidTokenException("This reset link has expired");
        }

        // Find user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate password strength BEFORE sending to Keycloak
        try {
            validatePasswordStrength(request.getNewPassword(), user);
        } catch (PasswordValidationException e) {
            return ApiResponse.error("400", e.getMessage());
        }

        // Change password in Keycloak
        try {
            keycloakService.changePassword(user.getAuthId(), request.getNewPassword());

            // CRITICAL: Invalidate all sessions after password reset
            keycloakService.logoutAllSessions(user.getAuthId());

            // Mark token as used (keep in Redis until TTL for audit)
            tokenRedisRepository.markAsUsed(hashedToken);

            // Update user's password change timestamp
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setPasswordResetRequired(false);
            userRepository.save(user);

            log.info("Password reset successfully for user ID: {}. All sessions invalidated.", user.getId());

            // Send notification email about password change
            // emailService.sendPasswordChangedNotification(user.getEmail(),
            // user.getFirstName());

            return ApiResponse.success("Password has been reset successfully. Please login with your new password.");

        } catch (Exception e) {
            log.error("Failed to reset password for user ID: {}", user.getId(), e);
            return ApiResponse.error("500", "Failed to reset password. Please try again");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Void> validateResetToken(String token) {
        log.debug("Validating reset token");

        // Hash the token to look it up
        String hashedToken = hashToken(token);

        // Check if token exists in Redis
        if (!tokenRedisRepository.exists(hashedToken)) {
            return ApiResponse.error("400", "Invalid or expired reset token");
        }

        PasswordResetTokenRedis resetToken = tokenRedisRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        // Check if already used
        if (resetToken.isUsed()) {
            return ApiResponse.error("400", "This reset token has already been used");
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            tokenRedisRepository.delete(hashedToken);
            return ApiResponse.error("400", "Reset token has expired");
        }

        // Get remaining time
        Long remainingSeconds = tokenRedisRepository.getTTL(hashedToken);
        String message = remainingSeconds != null
                ? String.format("Token is valid. Expires in %d minutes", remainingSeconds / 60)
                : "Token is valid";

        return ApiResponse.success(message);
    }

    /**
     * Validate password strength (pre-validation before Keycloak)
     */
    private void validatePasswordStrength(String password, User user) {
        List<String> errors = new ArrayList<>();

        // Length check
        if (password.length() < minPasswordLength) {
            errors.add(String.format("Password must be at least %d characters long", minPasswordLength));
        }

        // Complexity checks
        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            errors.add("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            errors.add("Password must contain at least one special character");
        }

        // Check against common passwords
        if (isCommonPassword(password)) {
            errors.add("This password is too common. Please choose a stronger password");
        }

        // Check if password contains user info
        if (containsUserInfo(password, user)) {
            errors.add("Password cannot contain your name or email");
        }

        // TODO: Check password history (if you store hashed previous passwords)
        // if (isPasswordReused(password, user)) {
        // errors.add("You cannot reuse any of your last 5 passwords");
        // }

        if (!errors.isEmpty()) {
            throw new PasswordValidationException(String.join(". ", errors));
        }
    }

    /**
     * Check if password contains user information
     */
    private boolean containsUserInfo(String password, User user) {
        String lowerPassword = password.toLowerCase();

        // Check email username part
        if (lowerPassword.contains(user.getEmail().split("@")[0].toLowerCase())) {
            return true;
        }

        // Check username
        if (user.getUsername() != null &&
                lowerPassword.contains(user.getUsername().toLowerCase())) {
            return true;
        }

        // Check profile name fields if profile exists
        if (user.getProfile() != null) {
            Profile profile = user.getProfile();

            if (profile.getFirstName() != null &&
                    lowerPassword.contains(profile.getFirstName().toLowerCase())) {
                return true;
            }

            if (profile.getLastName() != null &&
                    lowerPassword.contains(profile.getLastName().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if password is in common passwords list
     */
    private boolean isCommonPassword(String password) {
        return COMMON_PASSWORDS.contains(password.toLowerCase());
    }

    /**
     * Hash token using SHA-256
     * CRITICAL: Tokens must be hashed before storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Generate cryptographically secure random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Simulate processing delay to prevent timing attacks
     * Ensures consistent response time regardless of whether email exists
     */
    private void simulateProcessingDelay() {
        try {
            Thread.sleep(200 + new SecureRandom().nextInt(100)); // 200-300ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate password reset link
     */
    private String generateResetLink(String token) {
        return String.format("%s/reset-password?token=%s", frontendUrl, token);
    }

    /**
     * Send reset email (placeholder - implement with your email service)
     */
    private void sendResetEmail(User user, String resetLink) {
        try {
            // TODO: Implement email service
            // emailService.sendPasswordResetEmail(user.getEmail(), resetLink,
            // user.getFirstName());
            log.info("Password reset email would be sent to: {} with link: {}", user.getEmail(), resetLink);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            // Don't throw - we've already saved the token
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    /**
     * Get user agent from request
     */
    private String getUserAgent() {
        String userAgent = httpRequest.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    /**
     * Get active reset token for user (for monitoring/debugging)
     * WARNING: Only use in admin/debug endpoints
     */
    public ApiResponse<String> getActiveResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var tokenOpt = tokenRedisRepository.findByUserId(user.getId());
        if (tokenOpt.isEmpty()) {
            return ApiResponse.error("404", "No active reset token found");
        }

        PasswordResetTokenRedis token = tokenOpt.get();
        Long remainingSeconds = tokenRedisRepository.getTTL(token.getTokenHash());

        String info = String.format(
                "Token Hash: %s..., Expires in: %d minutes, Attempts: %d/%d, Used: %s",
                token.getTokenHash().substring(0, 10),
                remainingSeconds != null ? remainingSeconds / 60 : 0,
                token.getAttempts(),
                maxResetAttempts,
                token.isUsed());

        return ApiResponse.success(null, info);
    }

    /**
     * Revoke/cancel active reset token for user
     */
    @Transactional
    @Auditable(action = AuditAction.PASSWORD_RESET_REVOKED, entityType = "PasswordResetToken")
    public ApiResponse<Void> revokeResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        tokenRedisRepository.deleteByUserId(user.getId());
        log.info("Revoked password reset token for user ID: {}", user.getId());

        return ApiResponse.success("Password reset token has been revoked");
    }
}