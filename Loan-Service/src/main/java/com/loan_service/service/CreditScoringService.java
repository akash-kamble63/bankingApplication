package com.loan_service.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.loan_service.dto.CreditScoreResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {
	private final WebClient creditBureauWebClient;

	@CircuitBreaker(name = "credit-bureau", fallbackMethod = "creditScoreFallback")
	public CreditScoreResponse fetchCreditScore(Long userId, String panNumber) {
		log.info("Fetching credit score for user: {}", userId);

		try {
			Map<String, Object> response = creditBureauWebClient.post().uri("/api/credit-score")
					.bodyValue(Map.of("panNumber", panNumber, "userId", userId)).retrieve()
					.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
					}).block(Duration.ofSeconds(10));

			Integer score = (Integer) response.get("score");
			String bureau = (String) response.get("bureau");

			return CreditScoreResponse.builder().score(score).bureau(bureau).rating(getRating(score)).build();

		} catch (Exception e) {
			log.error("Failed to fetch credit score: {}", e.getMessage());
			throw e;
		}
	}

	public BigDecimal calculateInterestRate(Integer creditScore, BigDecimal requestedAmount, Integer tenure) {

		// Base rate
		BigDecimal baseRate = new BigDecimal("10.0");

		// Credit score adjustment
		BigDecimal scoreAdjustment = BigDecimal.ZERO;
		if (creditScore >= 800) {
			scoreAdjustment = new BigDecimal("-2.0"); // Discount
		} else if (creditScore >= 750) {
			scoreAdjustment = new BigDecimal("-1.0");
		} else if (creditScore < 650) {
			scoreAdjustment = new BigDecimal("3.0"); // Premium
		} else if (creditScore < 700) {
			scoreAdjustment = new BigDecimal("1.5");
		}

		// Amount adjustment (higher amount = lower rate)
		BigDecimal amountAdjustment = BigDecimal.ZERO;
		if (requestedAmount.compareTo(new BigDecimal("1000000")) > 0) {
			amountAdjustment = new BigDecimal("-0.5");
		}

		// Tenure adjustment (longer tenure = higher rate)
		BigDecimal tenureAdjustment = BigDecimal.ZERO;
		if (tenure > 240) {
			tenureAdjustment = new BigDecimal("1.0");
		}

		BigDecimal finalRate = baseRate.add(scoreAdjustment).add(amountAdjustment).add(tenureAdjustment);

		// Minimum 8%, Maximum 24%
		if (finalRate.compareTo(new BigDecimal("8.0")) < 0) {
			return new BigDecimal("8.0");
		}
		if (finalRate.compareTo(new BigDecimal("24.0")) > 0) {
			return new BigDecimal("24.0");
		}

		return finalRate;
	}

	private CreditScoreResponse creditScoreFallback(Long userId, String panNumber, Exception e) {
		log.warn("Credit bureau unavailable, using default score");
		return CreditScoreResponse.builder().score(700).bureau("DEFAULT").rating("FAIR").build();
	}

	private String getRating(Integer score) {
		if (score >= 800)
			return "EXCELLENT";
		if (score >= 750)
			return "VERY_GOOD";
		if (score >= 700)
			return "GOOD";
		if (score >= 650)
			return "FAIR";
		return "POOR";
	}
}
