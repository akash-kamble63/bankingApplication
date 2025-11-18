package com.card_service.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.card_service.client.AccountServiceClient;
import com.card_service.dto.CardActivationRequest;
import com.card_service.dto.CardBlockRequest;
import com.card_service.dto.CardControlsRequest;
import com.card_service.dto.CardIssueRequest;
import com.card_service.dto.CardLimitUpdateRequest;
import com.card_service.dto.CardResponse;
import com.card_service.dto.PinChangeRequest;
import com.card_service.entity.Card;
import com.card_service.enums.CardNetwork;
import com.card_service.enums.CardStatus;
import com.card_service.enums.CardType;
import com.card_service.exception.CardNotFoundException;
import com.card_service.exception.InvalidCardOperationException;
import com.card_service.exception.UnauthorizedAccessException;
import com.card_service.repository.CardRepository;
import com.card_service.service.CardLimitService;
import com.card_service.service.CardNumberGenerator;
import com.card_service.service.CardService;
import com.card_service.service.EncryptionService;
import com.card_service.service.EventSourcingService;
import com.card_service.service.OutboxService;
import com.card_service.service.TokenGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
	private final CardRepository cardRepository;
	private final EncryptionService encryptionService;
	private final CardNumberGenerator cardNumberGenerator;
	private final TokenGenerator tokenGenerator;
	private final CardLimitService limitService;
	private final AccountServiceClient accountServiceClient;
	private final OutboxService outboxService;
	private final EventSourcingService eventSourcingService;
	
	@Override
    @Transactional
    public CardResponse issueCard(CardIssueRequest request, Long userId) {
        log.info("Issuing card: userId={}, type={}, network={}", 
            userId, request.getCardType(), request.getCardNetwork());
        
        // Validate account ownership
        if (!accountServiceClient.validateAccountOwnership(request.getAccountId(), userId)) {
            throw new UnauthorizedAccessException("Account does not belong to user");
        }
        
        // Check user limits (max 5 cards per type)
        long activeCards = cardRepository.countByUserIdAndStatus(userId, CardStatus.ACTIVE);
        if (activeCards >= 5) {
            throw new InvalidCardOperationException("Maximum card limit reached");
        }
        
        // Generate card details
        String cardNumber = cardNumberGenerator.generateCardNumber(request.getCardNetwork());
        String cvv = cardNumberGenerator.generateCVV();
        String cardToken = tokenGenerator.generateCardToken();
        String cardReference = tokenGenerator.generateCardReference();
        String activationCode = cardNumberGenerator.generateActivationCode();
        
        // Encrypt sensitive data
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);
        String encryptedCvv = encryptionService.encrypt(cvv);
        String cardNumberHash = encryptionService.hash(cardNumber);
        
        // Calculate expiry (5 years from now)
        YearMonth expiry = YearMonth.now().plusYears(5);
        
        // Set default limits
        BigDecimal dailyLimit = request.getDailyLimit() != null ? 
            request.getDailyLimit() : getDefaultDailyLimit(request.getCardType());
        BigDecimal monthlyLimit = request.getMonthlyLimit() != null ?
            request.getMonthlyLimit() : dailyLimit.multiply(new BigDecimal("30"));
        
        // Create card entity
        Card card = Card.builder()
            .cardReference(cardReference)
            .userId(userId)
            .accountId(request.getAccountId())
            .cardNumberEncrypted(encryptedCardNumber)
            .cardNumberHash(cardNumberHash)
            .cardToken(cardToken)
            .cardHolderName(request.getCardHolderName())
            .cardType(request.getCardType())
            .cardNetwork(request.getCardNetwork())
            .cardVariant(request.getCardVariant())
            .isVirtual(request.getIsVirtual())
            .expiryMonth(expiry.getMonthValue())
            .expiryYear(expiry.getYear())
            .cvvEncrypted(encryptedCvv)
            .pinSet(false)
            .status(request.getIsVirtual() ? CardStatus.ACTIVE : CardStatus.PENDING_ACTIVATION)
            .dailyLimit(dailyLimit)
            .monthlyLimit(monthlyLimit)
            .perTransactionLimit(request.getPerTransactionLimit())
            .dailyWithdrawalLimit(new BigDecimal("50000"))
            .contactlessEnabled(true)
            .onlineTransactionsEnabled(true)
            .internationalTransactionsEnabled(false)
            .atmEnabled(true)
            .posEnabled(true)
            .threeDSecureEnabled(true)
            .rewardPoints(BigDecimal.ZERO)
            .activationCode(activationCode)
            .activationExpiry(LocalDateTime.now().plusDays(30))
            .issuedAt(LocalDateTime.now())
            .deliveryStatus(request.getIsVirtual() ? null : 
                com.card_service.enums.DeliveryStatus.PENDING)
            .deliveryAddress(request.getDeliveryAddress())
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        // Set credit card specific fields
        if (request.getCardType() == CardType.CREDIT) {
            BigDecimal creditLimit = request.getCreditLimit() != null ?
                request.getCreditLimit() : new BigDecimal("100000");
            card.setCreditLimit(creditLimit);
            card.setAvailableCredit(creditLimit);
            card.setOutstandingBalance(BigDecimal.ZERO);
            card.setBillingCycleDay(1);
            card.setPaymentDueDay(21);
            card.setMinimumPaymentPercentage(new BigDecimal("5.0"));
            card.setInterestRate(new BigDecimal("36.0")); // 3% monthly
        }
        
        card = cardRepository.save(card);
        
        // Store event
        eventSourcingService.storeEvent(
            cardReference,
            "CardIssued",
            buildCardIssuedEvent(card),
            userId,
            card.getCorrelationId(),
            null
        );
        
        // Publish to outbox
        outboxService.saveEvent(
            "CARD",
            cardReference,
            "CardIssued",
            "banking.card.events",
            card
        );
        
        log.info("Card issued successfully: reference={}, type={}", 
            cardReference, request.getCardType());
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse activateCard(String cardToken, CardActivationRequest request, 
                                    Long userId) {
        log.info("Activating card: token={}", cardToken);
        
        Card card = findCardByToken(cardToken, userId);
        
        if (card.getStatus() != CardStatus.PENDING_ACTIVATION) {
            throw new InvalidCardOperationException("Card is not pending activation");
        }
        
        // Verify activation code
        if (!card.getActivationCode().equals(request.getActivationCode())) {
            throw new InvalidCardOperationException("Invalid activation code");
        }
        
        // Check expiry
        if (card.getActivationExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidCardOperationException("Activation code expired");
        }
        
        // Set PIN
        String hashedPin = encryptionService.hashPin(request.getPin());
        card.setPinHash(hashedPin);
        card.setPinSet(true);
        
        // Activate card
        card.setStatus(CardStatus.ACTIVE);
        card.setActivatedAt(LocalDateTime.now());
        card.setActivationCode(null);
        
        card = cardRepository.save(card);
        
        // Store event
        eventSourcingService.storeEvent(
            card.getCardReference(),
            "CardActivated",
            Map.of("cardToken", cardToken, "userId", userId),
            userId,
            card.getCorrelationId(),
            null
        );
        
        // Publish event
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardActivated", "banking.card.events", card);
        
        log.info("Card activated successfully: reference={}", card.getCardReference());
        
        return mapToResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCard(String cardToken, Long userId) {
        Card card = findCardByToken(cardToken, userId);
        return mapToResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(Long userId, Pageable pageable) {
        Page<Card> cards = cardRepository.findByUserId(userId, pageable);
        return cards.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CardResponse blockCard(String cardToken, CardBlockRequest request, Long userId) {
        log.info("Blocking card: token={}, reason={}", cardToken, request.getReason());
        
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be blocked");
        }
        
        CardStatus newStatus = CardStatus.BLOCKED;
        if (request.getReportLost() != null && request.getReportLost()) {
            newStatus = CardStatus.LOST;
        } else if (request.getReportStolen() != null && request.getReportStolen()) {
            newStatus = CardStatus.STOLEN;
        }
        
        card.setStatus(newStatus);
        card.setBlockedAt(LocalDateTime.now());
        card.setBlockReason(request.getReason());
        
        card = cardRepository.save(card);
        
        // Store event
        eventSourcingService.storeEvent(
            card.getCardReference(),
            "CardBlocked",
            Map.of("reason", request.getReason(), "status", newStatus),
            userId,
            card.getCorrelationId(),
            null
        );
        
        // Publish event
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardBlocked", "banking.card.events", card);
        
        log.info("Card blocked: reference={}, status={}", card.getCardReference(), newStatus);
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse freezeCard(String cardToken, Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Only active cards can be frozen");
        }
        
        card.setStatus(CardStatus.FROZEN);
        card = cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardFrozen", "banking.card.events", card);
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse unfreezeCard(String cardToken, Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        if (card.getStatus() != CardStatus.FROZEN) {
            throw new InvalidCardOperationException("Card is not frozen");
        }
        
        card.setStatus(CardStatus.ACTIVE);
        card = cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardUnfrozen", "banking.card.events", card);
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse replaceCard(String cardToken, String reason, Long userId) {
        log.info("Replacing card: token={}, reason={}", cardToken, reason);
        
        Card oldCard = findCardByToken(cardToken, userId);
        
        // Create new card with same details
        CardIssueRequest issueRequest = CardIssueRequest.builder()
            .userId(userId)
            .accountId(oldCard.getAccountId())
            .cardHolderName(oldCard.getCardHolderName())
            .cardType(oldCard.getCardType())
            .cardNetwork(oldCard.getCardNetwork())
            .cardVariant(oldCard.getCardVariant())
            .isVirtual(oldCard.getIsVirtual())
            .dailyLimit(oldCard.getDailyLimit())
            .monthlyLimit(oldCard.getMonthlyLimit())
            .deliveryAddress(oldCard.getDeliveryAddress())
            .build();
        
        CardResponse newCard = issueCard(issueRequest, userId);
        
        // Link cards
        oldCard.setReplacedByCardId(newCard.getId());
        oldCard.setStatus(CardStatus.CLOSED);
        cardRepository.save(oldCard);
        
        return newCard;
    }

    @Override
    @Transactional
    public void changePin(String cardToken, PinChangeRequest request, Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        // Verify old PIN
        if (!encryptionService.verifyPin(request.getOldPin(), card.getPinHash())) {
            throw new InvalidCardOperationException("Invalid old PIN");
        }
        
        // Set new PIN
        String hashedNewPin = encryptionService.hashPin(request.getNewPin());
        card.setPinHash(hashedNewPin);
        
        cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardPinChanged", "banking.card.events", card);
    }

    @Override
    @Transactional
    public CardResponse updateLimits(String cardToken, CardLimitUpdateRequest request, 
                                    Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        if (request.getDailyLimit() != null) {
            card.setDailyLimit(request.getDailyLimit());
        }
        if (request.getMonthlyLimit() != null) {
            card.setMonthlyLimit(request.getMonthlyLimit());
        }
        if (request.getPerTransactionLimit() != null) {
            card.setPerTransactionLimit(request.getPerTransactionLimit());
        }
        
        card = cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardLimitsUpdated", "banking.card.events", card);
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateControls(String cardToken, CardControlsRequest request, 
                                      Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        
        if (request.getContactlessEnabled() != null) {
            card.setContactlessEnabled(request.getContactlessEnabled());
        }
        if (request.getOnlineTransactionsEnabled() != null) {
            card.setOnlineTransactionsEnabled(request.getOnlineTransactionsEnabled());
        }
        if (request.getInternationalTransactionsEnabled() != null) {
            card.setInternationalTransactionsEnabled(
                request.getInternationalTransactionsEnabled());
        }
        if (request.getAtmEnabled() != null) {
            card.setAtmEnabled(request.getAtmEnabled());
        }
        if (request.getPosEnabled() != null) {
            card.setPosEnabled(request.getPosEnabled());
        }
        
        card = cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardControlsUpdated", "banking.card.events", card);
        
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public String generateVirtualCard(Long accountId, Long userId) {
        CardIssueRequest request = CardIssueRequest.builder()
            .userId(userId)
            .accountId(accountId)
            .cardHolderName("Virtual Card User")
            .cardType(CardType.DEBIT)
            .cardNetwork(CardNetwork.VISA)
            .isVirtual(true)
            .dailyLimit(new BigDecimal("50000"))
            .build();
        
        CardResponse card = issueCard(request, userId);
        return card.getCardToken();
    }

    @Override
    @Transactional
    public void closeCard(String cardToken, Long userId) {
        Card card = findCardByTokenForUpdate(cardToken, userId);
        card.setStatus(CardStatus.CLOSED);
        cardRepository.save(card);
        
        outboxService.saveEvent("CARD", card.getCardReference(), 
            "CardClosed", "banking.card.events", card);
    }

    // Helper methods
    private Card findCardByToken(String cardToken, Long userId) {
        Card card = cardRepository.findByCardToken(cardToken)
            .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!card.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to card");
        }
        
        return card;
    }

    private Card findCardByTokenForUpdate(String cardToken, Long userId) {
        Card card = cardRepository.findByCardTokenForUpdate(cardToken)
            .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!card.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Unauthorized access to card");
        }
        
        return card;
    }

    private BigDecimal getDefaultDailyLimit(CardType cardType) {
        return switch (cardType) {
            case DEBIT -> new BigDecimal("100000");
            case CREDIT -> new BigDecimal("200000");
            case PREPAID -> new BigDecimal("50000");
        };
    }

    private CardResponse mapToResponse(Card card) {
        // Decrypt card number for masking
        String cardNumber = encryptionService.decrypt(card.getCardNumberEncrypted());
        String maskedNumber = maskCardNumber(cardNumber);
        
        return CardResponse.builder()
            .id(card.getId())
            .cardReference(card.getCardReference())
            .userId(card.getUserId())
            .accountId(card.getAccountId())
            .cardToken(card.getCardToken())
            .maskedCardNumber(maskedNumber)
            .cardHolderName(card.getCardHolderName())
            .cardType(card.getCardType())
            .cardNetwork(card.getCardNetwork())
            .cardVariant(card.getCardVariant())
            .isVirtual(card.getIsVirtual())
            .expiryDate(String.format("%02d/%02d", card.getExpiryMonth(), 
                card.getExpiryYear() % 100))
            .status(card.getStatus())
            .dailyLimit(card.getDailyLimit())
            .monthlyLimit(card.getMonthlyLimit())
            .dailySpent(card.getDailySpent())
            .monthlySpent(card.getMonthlySpent())
            .rewardPoints(card.getRewardPoints())
            .creditLimit(card.getCreditLimit())
            .availableCredit(card.getAvailableCredit())
            .contactlessEnabled(card.getContactlessEnabled())
            .onlineTransactionsEnabled(card.getOnlineTransactionsEnabled())
            .internationalTransactionsEnabled(card.getInternationalTransactionsEnabled())
            .lastUsedAt(card.getLastUsedAt())
            .createdAt(card.getCreatedAt())
            .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    private Map<String, Object> buildCardIssuedEvent(Card card) {
        return Map.of(
            "cardReference", card.getCardReference(),
            "userId", card.getUserId(),
            "cardType", card.getCardType().name(),
            "cardNetwork", card.getCardNetwork().name()
        );
    }
}
