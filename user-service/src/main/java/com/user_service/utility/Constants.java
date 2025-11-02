package com.user_service.utility;

public final class Constants {

private Constants() {} // Prevent instantiation
    
    // Response Codes
    public static final String SUCCESS = "200";
    public static final String BAD_REQUEST = "400";
    public static final String NOT_FOUND = "404";
    public static final String CONFLICT = "409";
    public static final String INTERNAL_ERROR = "500";
    
    // Response Messages
    public static final String USER_CREATED = "User registered successfully. Please check your email to verify your account.";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String EMAIL_ALREADY_EXISTS = "Email is already associated with another user";
    public static final String VERIFICATION_EMAIL_SENT = "Verification email sent successfully";
    
    // Keycloak
    public static final String VERIFY_EMAIL_ACTION = "VERIFY_EMAIL";
    public static final int KEYCLOAK_CREATED = 201;
    
    // Sync Job
    public static final String SYNC_JOB_NAME = "UserVerificationSyncJob";
}
