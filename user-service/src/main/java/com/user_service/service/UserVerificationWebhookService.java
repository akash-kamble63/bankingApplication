package com.user_service.service;

public interface UserVerificationWebhookService {
	public void handleEmailVerification(String keycloakUserId);
}
