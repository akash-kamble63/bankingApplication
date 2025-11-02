package com.user_service.exception;

public class ResourceNotFoundException extends GlobalException{

	public ResourceNotFoundException() {
		super("Resource not found on the server", GlobalError.NOT_FOUND);
	}
	
	public ResourceNotFoundException(String message) {
		super(message,GlobalError.NOT_FOUND);
	}
}
