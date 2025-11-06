package com.user_service.exception;

public class RateLimitExceededException extends GlobalException {

	public RateLimitExceededException() {
		super("Rate limit exceed", GlobalError.TOO_MANY_REQUESTS);
	}
	
	public RateLimitExceededException(String message) {
		super(message,GlobalError.TOO_MANY_REQUESTS);
	}
}
