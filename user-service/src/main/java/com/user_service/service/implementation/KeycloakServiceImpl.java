package com.user_service.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.user_service.config.KeyCloakManager;
import com.user_service.config.KeyCloakProp;
import com.user_service.exception.KeycloakOperationException;
import com.user_service.service.KeycloakService;
import com.user_service.config.WebClientConfig;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakServiceImpl implements KeycloakService {

	private final KeyCloakManager keyCloakManager;
	private final WebClient keycloakWebClient;
	private final KeyCloakProp keyclaCloakProp;

	@Override
	public Integer createUser(UserRepresentation userRepresentation) {
		log.info("Creating user in Keycloak: {}", userRepresentation.getEmail());

		try {
			Response response = keyCloakManager.getKeyCloakInstanceWithRealm().users().create(userRepresentation);

			int status = response.getStatus();
			response.close();

			log.info("Keycloak user creation response: {}", status);
			return status;

		} catch (Exception e) {
			log.error("Error creating user in Keycloak: {}", e.getMessage(), e);
			throw new KeycloakOperationException("Failed to create user in Keycloak", e);
		}
	}

	@Override
	public List<UserRepresentation> readUserByEmail(String email) {
		log.debug("Searching for user by email in Keycloak: {}", email);

		try {
			List<UserRepresentation> users = keyCloakManager.getKeyCloakInstanceWithRealm()
					.users()
					.search(email, true); // exact match

			log.debug("Found {} users with email: {}", users.size(), email);
			return users;

		} catch (Exception e) {
			log.error("Error searching user by email in Keycloak: {}", e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public void sendVerificationEmail(String userId) {
		log.info("Sending verification email for user ID: {}", userId);

		try {
			keyCloakManager.getKeyCloakInstanceWithRealm().users().get(userId).sendVerifyEmail();

			log.info("Verification email sent successfully for user ID: {}", userId);

		} catch (NotFoundException e) {
			log.error("User not found in Keycloak with ID: {}", userId);
			throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
		} catch (Exception e) {
			log.error("Error sending verification email for user ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to send verification email", e);
		}
	}

	@Override
	public UserRepresentation getUserById(String userId) {
		log.debug("Fetching user from Keycloak with ID: {}", userId);

		try {
			UserRepresentation user = keyCloakManager.getKeyCloakInstanceWithRealm()
					.users()
					.get(userId)
					.toRepresentation();

			log.debug("Successfully fetched user from Keycloak: {}", user.getEmail());
			return user;

		} catch (NotFoundException e) {
			log.warn("User not found in Keycloak with ID: {}", userId);
			return null;
		} catch (Exception e) {
			log.error("Error fetching user from Keycloak with ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to fetch user from Keycloak", e);
		}
	}

	@Override
	public void updateKeycloakUser(String userId, UserRepresentation userRepresentation) {
		log.info("Updating user in Keycloak: {}", userId);

		try {
			keyCloakManager.getKeyCloakInstanceWithRealm().users().get(userId).update(userRepresentation);

			log.info("User updated successfully in Keycloak: {}", userId);

		} catch (NotFoundException e) {
			log.error("User not found in Keycloak with ID: {}", userId);
			throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
		} catch (Exception e) {
			log.error("Error updating user in Keycloak with ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to update user in Keycloak", e);
		}
	}

	@Override
	public void changePassword(String userId, String newPassword) {
		log.info("Changing password for user ID: {}", userId);

		try {
			CredentialRepresentation credential = new CredentialRepresentation();
			credential.setType(CredentialRepresentation.PASSWORD);
			credential.setValue(newPassword);
			credential.setTemporary(false);

			keyCloakManager.getKeyCloakInstanceWithRealm().users().get(userId).resetPassword(credential);

			log.info("Password changed successfully for user ID: {}", userId);

		} catch (NotFoundException e) {
			log.error("User not found in Keycloak with ID: {}", userId);
			throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
		} catch (Exception e) {
			log.error("Error changing password for user ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to change password in Keycloak", e);
		}
	}

	@Override
	public void sendPasswordResetEmail(String userId) {
		log.info("Sending password reset email for user ID: {}", userId);

		try {
			keyCloakManager.getKeyCloakInstanceWithRealm()
					.users()
					.get(userId)
					.executeActionsEmail(Arrays.asList("UPDATE_PASSWORD"));

			log.info("Password reset email sent successfully for user ID: {}", userId);

		} catch (NotFoundException e) {
			log.error("User not found in Keycloak with ID: {}", userId);
			throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
		} catch (Exception e) {
			log.error("Error sending password reset email for user ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to send password reset email", e);
		}
	}

	@Override
	public boolean verifyPassword(String userId, String password) {
		log.debug("Verifying password for user ID: {}", userId);
		try {
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("grant_type", "password");
			formData.add("client_id", keyclaCloakProp.getClientId());
			formData.add("client_secret", keyclaCloakProp.getClientSecret());
			formData.add("username", userId);
			formData.add("password", password);

			keycloakWebClient.post()
					.uri("/realms/{realm}/protocol/openid-connect/token", keyclaCloakProp.getRealm())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.bodyValue(formData)
					.retrieve()
					.bodyToMono(String.class)
					.block();

			// If token is returned → password is valid
			return true;

		} catch (WebClientResponseException.Unauthorized e) {
			// Invalid credentials
			return false;
		} catch (Exception e) {
			log.error("Error verifying password with Keycloak", e);
			return false;
		}
	}

	/**
	 * CRITICAL SECURITY: Logout all sessions for a user
	 * This should be called after password reset to invalidate all existing
	 * sessions
	 */
	@Override
	public void logoutAllSessions(String userId) {
		log.info("Logging out all sessions for user ID: {}", userId);

		try {
			UserResource userResource = keyCloakManager.getKeyCloakInstanceWithRealm()
					.users()
					.get(userId);

			// Get all active sessions for the user
			List<UserSessionRepresentation> sessions = userResource.getUserSessions();

			log.info("Found {} active sessions for user ID: {}", sessions.size(), userId);

			// Simply call logout on the user resource
			// This will invalidate all sessions for the user
			userResource.logout();

			log.info("All sessions invalidated successfully for user ID: {}", userId);

		} catch (NotFoundException e) {
			log.error("User not found in Keycloak with ID: {}", userId);
			throw new KeycloakOperationException("User not found in Keycloak: " + userId, e);
		} catch (Exception e) {
			log.error("Error logging out all sessions for user ID {}: {}", userId, e.getMessage(), e);
			throw new KeycloakOperationException("Failed to logout all sessions", e);
		}
	}

	/**
	 * Get count of active sessions for a user (for monitoring)
	 */
	public int getActiveSessionCount(String userId) {
		try {
			UserResource userResource = keyCloakManager.getKeyCloakInstanceWithRealm()
					.users()
					.get(userId);

			List<UserSessionRepresentation> sessions = userResource.getUserSessions();
			return sessions.size();

		} catch (Exception e) {
			log.error("Error getting session count for user ID {}: {}", userId, e.getMessage());
			return 0;
		}
	}
}