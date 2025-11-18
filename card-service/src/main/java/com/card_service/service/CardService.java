package com.card_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.card_service.dto.CardActivationRequest;
import com.card_service.dto.CardBlockRequest;
import com.card_service.dto.CardControlsRequest;
import com.card_service.dto.CardIssueRequest;
import com.card_service.dto.CardLimitUpdateRequest;
import com.card_service.dto.CardResponse;
import com.card_service.dto.PinChangeRequest;

public interface CardService {
	CardResponse issueCard(CardIssueRequest request, Long userId);

	CardResponse activateCard(String cardToken, CardActivationRequest request, Long userId);

	CardResponse getCard(String cardToken, Long userId);

	Page<CardResponse> getUserCards(Long userId, Pageable pageable);

	CardResponse blockCard(String cardToken, CardBlockRequest request, Long userId);

	CardResponse freezeCard(String cardToken, Long userId);

	CardResponse unfreezeCard(String cardToken, Long userId);

	CardResponse replaceCard(String cardToken, String reason, Long userId);

	void changePin(String cardToken, PinChangeRequest request, Long userId);

	CardResponse updateLimits(String cardToken, CardLimitUpdateRequest request, Long userId);

	CardResponse updateControls(String cardToken, CardControlsRequest request, Long userId);

	String generateVirtualCard(Long accountId, Long userId);

	void closeCard(String cardToken, Long userId);
}
