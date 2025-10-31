package com.user_service.service.implementation;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.user_service.dto.AuthResponse;
import com.user_service.dto.UserLogInRequestDTO;
import com.user_service.dto.UserSignUpRequestDTO;
import com.user_service.service.AuthService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {@Override
	public AuthResponse registerUser(UserSignUpRequestDTO request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthResponse LoginUser(UserLogInRequestDTO request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Authentication authenticate(String username, String password) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

//	private final KeycloakService keycloakService;
//	private final UserRepository userRepository;
//
//	@Value("${spring.application.success}")
//	private String responseCodeSuccess;
//
//	@Value("${spring.application.not_found}")
//	private String responseCodeNotFound;
//
//	// Frontend → user-service → create user in Keycloak → get Keycloak user ID →
//	// save user in DB → return response
//
//	@Override
//	public Response registerUser(CreateUser request) throws Exception {
//		List<UserRepresentation> userRepresentations = keycloakService.readUserByEmail(request.getEmail());
//		if(userRepresentations.size() > 0) {
//			 log.error("This emailId is already registered as a user");
//	            throw new ResourceConflictException("This emailId is already registered as a user");
//		}
//
//		return null;
//	}
//
//	@Override
//	public AuthResponse LoginUser(UserLogInRequestDTO request) throws Exception {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Authentication authenticate(String username, String password) throws Exception {
//		// TODO Auto-generated method stub
//		return null;
//	}

	
	
	
}
