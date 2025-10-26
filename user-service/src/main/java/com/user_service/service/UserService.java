package com.user_service.service;

import com.user_service.dto.response.CreateUser;
import com.user_service.dto.response.Response;

public interface UserService {
	Response createUser(CreateUser dto);
}
