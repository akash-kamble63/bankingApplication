package com.card_service.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceClient {
	private final WebClient accountServiceWebClient;

	/**
	 * Validate that the account belongs to the user Calls Account Service API
	 */
	public boolean validateAccountOwnership(Long accountId, Long userId) {
		try {
			log.debug("Validating account ownership: accountId={}, userId={}", accountId, userId);

			Boolean isOwner = accountServiceWebClient.get()
					.uri("/api/v1/accounts/{accountId}/validate-owner/{userId}", accountId, userId).retrieve()
					.bodyToMono(Boolean.class).block();

			boolean result = Boolean.TRUE.equals(isOwner);
			log.debug("Account ownership validation result: {}", result);
			return result;

		} catch (WebClientResponseException.NotFound e) {
			log.warn("Account not found: accountId={}", accountId);
			return false;
		} catch (WebClientResponseException.Forbidden e) {
			log.warn("Forbidden access to account: accountId={}, userId={}", accountId, userId);
			return false;
		} catch (Exception e) {
			log.error("Failed to validate account ownership: accountId={}, userId={}, error={}", accountId, userId,
					e.getMessage());
			// Fail secure - return false if we can't validate
			return false;
		}
	}

	/**
	 * Get account balance (optional - for additional validation)
	 */
	public AccountBalance getAccountBalance(Long accountId) {
		try {
			return accountServiceWebClient.get().uri("/api/v1/accounts/{accountId}/balance", accountId).retrieve()
					.bodyToMono(AccountBalance.class).block();
		} catch (Exception e) {
			log.error("Failed to get account balance: accountId={}, error={}", accountId, e.getMessage());
			return null;
		}
	}

// DTO for account balance
	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class AccountBalance {
		private Long accountId;
		private String accountNumber;
		private java.math.BigDecimal balance;
		private String currency;
		private String status;
	}
}
