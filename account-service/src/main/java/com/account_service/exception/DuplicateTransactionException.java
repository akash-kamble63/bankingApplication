package com.account_service.exception;

public class DuplicateTransactionException extends GlobalException{
	public DuplicateTransactionException() {
		super("Duplicate Transaction", GlobalError.NOT_FOUND);
	}
	
	public DuplicateTransactionException(String message) {
		super(message,GlobalError.NOT_FOUND);
	}
}
