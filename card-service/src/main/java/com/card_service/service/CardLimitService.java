package com.card_service.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.card_service.entity.Card;
import com.card_service.exception.CardLimitExceededException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardLimitService {
	public void validateTransactionLimit(Card card, BigDecimal amount) {
		// Check per-transaction limit
		if (card.getPerTransactionLimit() != null && amount.compareTo(card.getPerTransactionLimit()) > 0) {
			throw new CardLimitExceededException("Transaction exceeds per-transaction limit");
		}

		// Check daily limit
		BigDecimal newDailySpent = card.getDailySpent().add(amount);
		if (newDailySpent.compareTo(card.getDailyLimit()) > 0) {
			throw new CardLimitExceededException("Daily limit exceeded");
		}

		// Check monthly limit
		BigDecimal newMonthlySpent = card.getMonthlySpent().add(amount);
		if (newMonthlySpent.compareTo(card.getMonthlyLimit()) > 0) {
			throw new CardLimitExceededException("Monthly limit exceeded");
		}

		// Check credit limit (for credit cards)
		if (card.getAvailableCredit() != null) {
			if (amount.compareTo(card.getAvailableCredit()) > 0) {
				throw new CardLimitExceededException("Insufficient credit available");
			}
		}
	}

	public void updateLimitsAfterTransaction(Card card, BigDecimal amount) {
		card.setDailySpent(card.getDailySpent().add(amount));
		card.setMonthlySpent(card.getMonthlySpent().add(amount));

		if (card.getAvailableCredit() != null) {
			card.setAvailableCredit(card.getAvailableCredit().subtract(amount));
			card.setOutstandingBalance(card.getOutstandingBalance().add(amount));
		}

		card.setLastUsedAt(java.time.LocalDateTime.now());
	}
}
