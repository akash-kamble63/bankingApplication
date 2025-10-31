package com.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.CreateUser;
import com.user_service.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/users")
public class UserController {
	
	private final UserService userService;
	
	@GetMapping
	public ResponseEntity<?> getUserProfile(){
		
		return ResponseEntity.ok("");
	}
	
	@PostMapping("/register")
	public ResponseEntity<?> createUser(@RequestBody CreateUser userDTO){
		log.info("creating user with: {}", userDTO.toString());
		
		return ResponseEntity.ok(userService.createUser(userDTO));
	}
	
	
}
