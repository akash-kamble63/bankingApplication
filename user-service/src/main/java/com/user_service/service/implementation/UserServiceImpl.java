package com.user_service.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.response.Response;
import com.user_service.enums.Status;
import com.user_service.exception.ResourceConflictException;
import com.user_service.model.Profile;
import com.user_service.model.Users;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final KeycloakService keycloakService;
	private final KeyCloakManager keyCloakManager;

	@Value("${app.codes.success}")
	private String responseCodeSuccess;
	@Value("${app.codes.not_found}")
	private String responseCodeNodeFound;

	@Override
	public Response createUser(CreateUserRequest userDto) {

		// Check if user already exists
		List<UserRepresentation> userRepresentations = keycloakService.readUserByEmail(userDto.getEmailId());
		if (!userRepresentations.isEmpty()) {
			log.error("Email is already associated with other user");
			throw new ResourceConflictException("Email is already associated with other user");
		}

		// Create UserRepresentation
		UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setUsername(userDto.getEmailId());
		userRepresentation.setFirstName(userDto.getFirstName());
		userRepresentation.setLastName(userDto.getLastName());
		userRepresentation.setEmailVerified(false); // ✅ Not verified initially
		userRepresentation.setEnabled(true); // ✅ Enabled but requires actions
		userRepresentation.setEmail(userDto.getEmailId());

		// Set required action for email verification
		userRepresentation.setRequiredActions(Arrays.asList("VERIFY_EMAIL")); // ✅ ADDED THIS!

		// Create Credential
		CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
		credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
		credentialRepresentation.setValue(userDto.getPassword());
		credentialRepresentation.setTemporary(false);
		userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

		// Create user in Keycloak
		Integer userCreationResponse = keycloakService.createUser(userRepresentation);

		if (userCreationResponse.equals(201)) {
			List<UserRepresentation> representations = keycloakService.readUserByEmail(userDto.getEmailId());

			// Send verification email
			try {
				sendVerificationEmail(representations.get(0).getId());
				log.info("Verification email sent to: {}", userDto.getEmailId());
			} catch (Exception e) {
				log.error("Failed to send verification email: {}", e.getMessage());
				// Continue even if email fails - user can resend later
			}

			Profile profile = Profile.builder().firstName(userDto.getFirstName()).lastName(userDto.getLastName())
					.build();

			Users user = Users.builder().username(userDto.getEmailId()).email(userDto.getEmailId())
					.contactNo(userDto.getContactNumber()).status(Status.PENDING) // User is pending until email
																					// verified
					.profile(profile).authId(representations.get(0).getId())
					.identificationNumber(UUID.randomUUID().toString()).build();

			userRepository.save(user);

			log.info("User created successfully with authId: {}", representations.get(0).getId());

			return Response.builder().responseCode(responseCodeSuccess)
					.responseMessage("User registered successfully. Please check your email to verify your account.")
					.build();
		}

		log.error("Failed to create user in Keycloak. Status code: {}", userCreationResponse);
		throw new RuntimeException("Failed to create user. Status: " + userCreationResponse);
	}

	// Add this method to send verification email
	private void sendVerificationEmail(String userId) {
		try {
			keyCloakManager.getKeyCloakInstanceWithRealm().users().get(userId).sendVerifyEmail();
		} catch (Exception e) {
			log.error("Error sending verification email: {}", e.getMessage());
			throw e;
		}
	}

}
