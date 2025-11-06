package com.account_service.exception;

public class ResourceConflictException extends GlobalException{

	public ResourceConflictException() {
		super("Resource already present !");
	}
	
	public ResourceConflictException(String message) {
        super(message);
    }
}
