package com.user_service.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.user_service.controller.UserAuthController;
import com.user_service.dto.AuthResponse;
import com.user_service.dto.UserLogInRequestDTO;
import com.user_service.dto.UserSignUpRequestDTO;
import com.user_service.enums.Roles;
import com.user_service.mapper.UserMapper;
import com.user_service.model.Users;
import com.user_service.repository.UserRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthController userAuthController;

	private final UserRepository userRepository;
	
	public AuthServiceImpl(UserRepository userRepository, UserAuthController userAuthController) {
		this.userRepository = userRepository;
		this.userAuthController = userAuthController;
	}
	
	
	@Override
	public AuthResponse registerUser(UserSignUpRequestDTO request) throws Exception {
		// TODO Auto-generated method stub
		
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		
		String username = request.getUsername().trim();
		
		if(userRepository.findByEmail(normalizedEmail).isPresent()) {
			throw new Exception("Email Already Exist");
		}
		if(userRepository.findByUserName(username).isPresent()) {
			throw new Exception("Username Already Exist");
		}
		Roles role = Roles.valueOf(request.getRole().toUpperCase());
		
		Users user = UserMapper.INSTANCE.mapUser(request);
		user.setRole(role);
		user.setUsername(request.getUsername());
		
		
		userRepository.save(user);
	
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

}
