package com.user_service.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.dto.ApiResponse;
import com.user_service.dto.ChangePasswordRequest;
import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.dto.UpdateUserRequest;
import com.user_service.dto.UserResponse;
import com.user_service.enums.UserStatus;
import com.user_service.exception.ResourceConflictException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.UserMapper;
import com.user_service.model.Profile;
import com.user_service.model.User;
import com.user_service.repository.UserRepository;
import com.user_service.service.KeycloakService;
import com.user_service.service.UserService;
import com.user_service.utility.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final KeycloakService keycloakService;
	private final UserMapper userMapper;

	@Value("${app.codes.success}")
	private String responseCodeSuccess;
	@Value("${app.codes.not_found}")
	private String responseCodeNodeFound;

	// ===============================CREATE
	// METHODS=====================================

	@Override
	@Transactional
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


	// ====================================READ METHODS=====================================

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<Page<UserResponse>> getUsersByStatus(UserStatus status, Pageable pageable) {
		log.info("Fetching users by status: {} - Page: {}, Size: {}", status, pageable.getPageNumber(),
				pageable.getPageSize());

		Page<User> users = userRepository.findByStatus(status, pageable);
		Page<UserResponse> userResponses = users.map(userMapper::toResponse);

		return ApiResponse.success(userResponses, "Users retrieved successfully");
	}
	
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<UserResponse> getUserByEmail(String email) {
		log.info("Fetching user by email: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));

		UserResponse response = userMapper.toResponse(user);
		return ApiResponse.success(response, "User found");
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<UserResponse> getUserByAuthId(String authId) {
		log.info("Feching user details by AuthID:{}", authId);
		User user = userRepository.findByAuthId(authId)
				.orElseThrow(() -> new ResourceNotFoundException("User Does not found"));
		UserResponse response = userMapper.toResponse(user);

		return ApiResponse.success(response, "User Details retrieved successfully.");
	}

	@Override
	@Transactional(readOnly = true	)
	@Cacheable(value = "users", key = "#userId")
	public ApiResponse<UserResponse> getUserById(Long userId) {
		log.info("Feching user details by UserID:{}", userId);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User Does not exist"));
		UserResponse response = userMapper.toResponse(user);
		return ApiResponse.success(response, "User Details retrieved successfully.");

	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<UserResponse> getCurrentUser(String email) {
		log.info("Fetching current use deatils by email{}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

		UserResponse response = userMapper.toResponse(user);
		return ApiResponse.success(response, "Current user details retrieved successfully");

	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<Page<UserResponse>> getAllUsers(Pageable pageable) {
		log.info("Fetching all users - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

		Page<User> users = userRepository.findAll(pageable);
		Page<UserResponse> userResponses = users.map(userMapper::toResponse);

		return ApiResponse.success(userResponses, "Users retrieved successfully");
	}

	// ==================================UPDATE METHODS======================================

	@Override
	@Transactional
	@CacheEvict(value = "users", key = "#userId")

	public ApiResponse<UserResponse> updateUser(Long userId, UpdateUserRequest request) {
		log.info("Updating user with ID: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

		if (request.getFirstName() != null) {
			user.getProfile().setFirstName(request.getFirstName());
		}
		if (request.getLastName() != null) {
			user.getProfile().setLastName(request.getLastName());
		}
		if (request.getContactNumber() != null) {
			user.setContactNo(request.getContactNumber());
		}

		try {
			updateKeycloakUser(user.getAuthId(), request);
		} catch (Exception e) {
			log.error("Failed to update user in Keycloak: {}", e.getMessage());
		}

		User updatedUser = userRepository.save(user);
		log.info("User updated successfully: {}", userId);

		UserResponse response = userMapper.toResponse(updatedUser);
		return ApiResponse.success(response, "User updated successfully");

	}

	@Override
	@Transactional
	public ApiResponse<UserResponse> updateCurrentUser(String email, UpdateUserRequest request) {
		log.info("Current user updated by email: {}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not Found"));

		return updateUser(user.getId(), request);
	}

	@Override
	@Transactional
	public ApiResponse<UserResponse> updateUserProfile(Long userId, UpdateProfileRequest request) {
		log.info("Update user profile for userId: {}", userId);
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not Found"));

		Profile profile = user.getProfile();
		if (request.getGender() != null) {
			profile.setGender(request.getGender());
		}
		if (request.getAddress() != null) {
			profile.setAddress(request.getAddress());
		}
		if (request.getMaritalStatus() != null) {
			profile.setMaritalStatus(request.getMaritalStatus());
		}
		if (request.getNationality() != null) {
			profile.setNationality(request.getNationality());
		}
		if (request.getOccupation() != null) {
			profile.setOccupation(request.getOccupation());
		}

		User updatedUser = userRepository.save(user);
		UserResponse response = userMapper.toResponse(updatedUser);
		return ApiResponse.success(response, "User profile updated successfully");
	}

	@Override
	@Transactional
	public ApiResponse<UserResponse> updateCurrentUserProfile(String email, UpdateProfileRequest request) {
		log.info("Updating profile current user:{}", email);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return updateUserProfile(user.getId(), request);
	}

	// ================================DELETE METHODS=================================

	@Override
	@Transactional
	public ApiResponse<Void> deleteUser(Long userId) {
		log.info("DEleting the user :{}", userId);
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
		user.setStatus(UserStatus.DELETED);
		userRepository.save(user);
		// disable in keycloak
		try {

			UserRepresentation keycloakUser = keycloakService.getUserById(user.getAuthId());
			if (keycloakUser != null) {
				keycloakUser.setEnabled(false);
				keycloakService.updateKeycloakUser(user.getAuthId(), keycloakUser);
			}

		} catch (Exception ex) {
			log.info("failed to disable user in keycloak: {}", ex.getMessage());
		}
		log.info("User deleted successfully : {}", userId);
		return ApiResponse.success("User deleted successfully");
	}

	@Override
	@Transactional
	public ApiResponse<Void> deactivateUser(Long userId) {
		log.info("Deactivating user with ID: {}", userId);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
		user.setStatus(UserStatus.INACTIVE);
		userRepository.save(user);
		try {
			UserRepresentation keycloakUser = keycloakService.getUserById(user.getAuthId());
			if (keycloakUser != null) {
				keycloakUser.setEnabled(false);
				keycloakService.updateKeycloakUser(user.getAuthId(), keycloakUser);

			}
		} catch (Exception ex) {
			log.error("Failed to disable user in Keycloak: {}", ex.getMessage());
		}

		log.info("User deactivated successfully: {}", userId);

		return ApiResponse.success("User deleted successfully");
	}

	@Override
	@Transactional
	public ApiResponse<Void> activateUser(Long userId) {
		log.info("Activating user with ID: {}", userId);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
		user.setStatus(UserStatus.ACTIVE);
		if (user.getEmailVerifiedAt() == null) {
			return ApiResponse.error("400", "Cannot activate user - email not verified");
		}
		userRepository.save(user);
		try {
			UserRepresentation keycloakUser = keycloakService.getUserById(user.getAuthId());
			if (keycloakUser != null) {
				keycloakUser.setEnabled(true);
				keycloakService.updateKeycloakUser(user.getAuthId(), keycloakUser);
			}

		} catch (Exception ex) {
			log.error("Failed to enable user in Keycloak: {}", ex.getMessage());
		}
		log.info("User activated successfully: {}", userId);
		return ApiResponse.success("User Activated Successfully");
	}

	// ==================================== OTHER ===========================================

	@Override
	@Transactional
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

	// ==================== HELPER METHODS ====================

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

	private void updateKeycloakUser(String authId, UpdateUserRequest request) {
		UserRepresentation keycloakUser = keycloakService.getUserById(authId);

		if (keycloakUser != null) {
			if (request.getFirstName() != null) {
				keycloakUser.setFirstName(request.getFirstName());
			}
			if (request.getLastName() != null) {
				keycloakUser.setLastName(request.getLastName());
			}

			keycloakService.updateKeycloakUser(authId, keycloakUser);
		}
	}


	

}
