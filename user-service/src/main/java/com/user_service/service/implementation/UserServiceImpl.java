package com.user_service.service.implementation;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.user_service.config.KeyCloakManager;
import com.user_service.dto.CreateUser;
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

    private final KeyCloakManager keyCloakManager;
	
	private final UserRepository userRepository;
	private final KeycloakService keycloakService;
	
	@Value("${app.codes.success}")
	private String responseCodeSuccess;
	@Value("${app.codes.not_found}")
	private String responseCodeNodeFound;


	@Override
	public Response createUser(CreateUser userDto) {
		
		List<UserRepresentation> userRepresentations =  keycloakService.readUserByEmail(userDto.getEmailId());
		if(userRepresentations.size() > 0) {
			log.error("Email is already associated with other user");
			throw new ResourceConflictException("Email is already associated with other user");
		}
		
		
		UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setUsername(userDto.getEmailId());
		userRepresentation.setFirstName(userDto.getFirstName());
		userRepresentation.setLastName(userDto.getLastName());
		userRepresentation.setEmailVerified(false);
		userRepresentation.setEnabled(false);
		userRepresentation.setEmail(userDto.getEmailId());
		
		CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
		
		credentialRepresentation.setValue(userDto.getPassword());
		credentialRepresentation.setTemporary(false);
		userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));
		
		Integer userCreationResponse = keycloakService.createUser(userRepresentation);
		
		if(userCreationResponse.equals(201)) {
			List<UserRepresentation> representations = keycloakService.readUserByEmail(userDto.getEmailId());
			Profile profile = Profile.builder()
					.firstName(userDto.getFirstName())
					.lastName(userDto.getLastName())
					.build();
			
			Users user = Users.builder()
					.email(userDto.getEmailId())
					.contactNo(userDto.getContactNumber())
					.status(Status.PENDING)
					.userProfile(profile)
					.authId(representations.get(0).getId())
					.identificationNumber(UUID.randomUUID().toString())
					.build();
			
			userRepository.save(user);
			
			return Response.builder()
					.responseCode(responseCodeSuccess)
					.responseMessage("User created successfully")
					.build();
			
		}
		
		throw new RuntimeException("User with identification number not found");
	}


}
