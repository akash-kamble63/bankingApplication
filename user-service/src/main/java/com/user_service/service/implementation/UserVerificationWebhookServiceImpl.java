package com.user_service.service.implementation;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.user_service.enums.UserStatus;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.UserVerificationWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserVerificationWebhookServiceImpl implements UserVerificationWebhookService {
	private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    public void handleEmailVerification(String keycloakUserId) {
        log.info("Processing email verification for user ID: {}", keycloakUserId);
        
        try {
            User user = userRepository.findByAuthId(keycloakUserId)
                .orElseThrow(() -> new RuntimeException("User not found with authId: " + keycloakUserId));
            
            UserRepresentation keycloakUser = keycloakService.getUserById(keycloakUserId);
            
            if (keycloakUser != null && Boolean.TRUE.equals(keycloakUser.isEmailVerified())) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                log.info("User {} status updated to ACTIVE", user.getEmail());
            }
            
        } catch (Exception e) {
            log.error("Error handling email verification webhook: {}", e.getMessage());
        }
    }

}
