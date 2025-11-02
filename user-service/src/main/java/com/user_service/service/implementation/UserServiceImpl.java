package com.user_service.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.dto.ApiResponse;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UserResponse;
import com.user_service.enums.UserStatus;
import com.user_service.exception.ResourceConflictException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.UserMapper;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.UserService;
import com.user_service.utility.Constants;

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
	private final UserMapper userMapper;
	@Value("${app.codes.success}")
	private String responseCodeSuccess;
	@Value("${app.codes.not_found}")
	private String responseCodeNodeFound;

	@Override
	public ApiResponse<UserResponse> createUser(CreateUserRequest request) {
		log.info("Creating user with email: {}", request.getEmailId());

		// Check if user already exists
		if (userRepository.existsByEmail(request.getEmailId())) {
			log.error("Email already exists: {}", request.getEmailId());
			throw new ResourceConflictException(Constants.EMAIL_ALREADY_EXISTS);
		}

		// Create Keycloak user representation
		UserRepresentation keycloakUser = buildKeycloakUser(request);

		// Create user in Keycloak
		Integer status = keycloakService.createUser(keycloakUser);

		if (!status.equals(Constants.KEYCLOAK_CREATED)) {
			log.error("Failed to create user in Keycloak. Status: {}", status);
			throw new RuntimeException("Failed to create user in authentication service");
		}

		// Get created user from Keycloak to get the ID
		List<UserRepresentation> keycloakUsers = keycloakService.readUserByEmail(request.getEmailId());
		if (keycloakUsers.isEmpty()) {
			throw new RuntimeException("User created in Keycloak but not found");
		}

		String keycloakUserId = keycloakUsers.get(0).getId();
		log.info("User created in Keycloak with ID: {}", keycloakUserId);

		// Send verification email
		try {
			keycloakService.sendVerificationEmail(keycloakUserId);
			log.info("Verification email sent to: {}", request.getEmailId());
		} catch (Exception e) {
			log.error("Failed to send verification email: {}", e.getMessage());
			// Continue - user is created, they can resend email later
		}

		// Map request to entity and set Keycloak ID
		User user = userMapper.toEntity(request);
		user.setAuthId(keycloakUserId);
		user.setStatus(UserStatus.PENDING);

		// Save to database
		User savedUser = userRepository.save(user);
		log.info("User saved to database with ID: {}", savedUser.getId());

		// Map to response
		UserResponse userResponse = userMapper.toResponse(savedUser);

		return ApiResponse.success(userResponse, Constants.USER_CREATED);
	}

	/**
	 * Build Keycloak UserRepresentation from CreateUserRequest
	 */
	private UserRepresentation buildKeycloakUser(CreateUserRequest request) {
		UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setUsername(request.getEmailId());
		userRepresentation.setFirstName(request.getFirstName());
		userRepresentation.setLastName(request.getLastName());
		userRepresentation.setEmail(request.getEmailId());
		userRepresentation.setEmailVerified(false);
		userRepresentation.setEnabled(true);
		userRepresentation.setRequiredActions(Arrays.asList(Constants.VERIFY_EMAIL_ACTION));

		// Set password credential
		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(request.getPassword());
		credential.setTemporary(false);
		userRepresentation.setCredentials(Collections.singletonList(credential));

		return userRepresentation;
	}

	@Override
	public ApiResponse<UserResponse> getUserByEmail(String email) {
		log.info("Fetching user by email: {}", email);
	    
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));
	    
	    UserResponse response = userMapper.toResponse(user);
	    return ApiResponse.success(response, "User found");
	}

	@Override
	public ApiResponse<Void> resendVerificationEmail(String email) {
		log.info("Resending verification email to: {}", email);
	    
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));
	    
	    if (user.getStatus() == UserStatus.ACTIVE) {
	        return ApiResponse.error("400", "Email already verified");
	    }
	    
	    keycloakService.sendVerificationEmail(user.getAuthId());
	    
	    return ApiResponse.success(Constants.VERIFICATION_EMAIL_SENT);
	}
	
	

}
