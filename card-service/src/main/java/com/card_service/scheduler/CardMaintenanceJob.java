package com.card_service.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.card_service.entity.Card;
import com.card_service.repository.CardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardMaintenanceJob {
	private final CardRepository cardRepository;

	@Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
	@Transactional
	public void resetDailyLimits() {
		LocalDate today = LocalDate.now();
		List<Card> cards = cardRepository.findCardsNeedingReset(today);

		if (!cards.isEmpty()) {
			List<Long> cardIds = cards.stream().map(Card::getId).collect(Collectors.toList());
			cardRepository.resetDailyLimits(cardIds, today);
			log.info("Reset daily limits for {} cards", cards.size());
		}
	}

	@Scheduled(cron = "0 0 0 1 * ?") // Monthly on 1st day
	@Transactional
	public void resetMonthlyLimits() {
		List<Card> cards = cardRepository.findAll();
		List<Long> cardIds = cards.stream().map(Card::getId).collect(Collectors.toList());

		if (!cardIds.isEmpty()) {
			cardRepository.resetMonthlyLimits(cardIds);
			log.info("Reset monthly limits for {} cards", cards.size());
		}
	}

	@Scheduled(fixedDelay = 3600000) // Every hour
	@Transactional
	public void expirePendingActivations() {
		List<Card> expired = cardRepository.findExpiredPendingActivations(LocalDateTime.now());

		for (Card card : expired) {
			card.setStatus(com.card_service.enums.CardStatus.EXPIRED);
			cardRepository.save(card);
		}

		if (!expired.isEmpty()) {
			log.info("Expired {} pending card activations", expired.size());
		}
	}

}
