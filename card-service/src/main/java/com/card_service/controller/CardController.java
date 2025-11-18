package com.card_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.card_service.dto.CardActivationRequest;
import com.card_service.dto.CardBlockRequest;
import com.card_service.dto.CardControlsRequest;
import com.card_service.dto.CardIssueRequest;
import com.card_service.dto.CardLimitUpdateRequest;
import com.card_service.dto.CardResponse;
import com.card_service.dto.PinChangeRequest;
import com.card_service.service.CardService;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Card management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CardController {
	private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Timed(value = "card.issue", description = "Time to issue card")
    @Operation(summary = "Issue new card", description = "Issue a new debit/credit card")
    public ResponseEntity<CardResponse> issueCard(
            @Valid @RequestBody CardIssueRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.issueCard(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PostMapping("/{cardToken}/activate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Activate card", description = "Activate card with activation code")
    public ResponseEntity<CardResponse> activateCard(
            @PathVariable String cardToken,
            @Valid @RequestBody CardActivationRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.activateCard(cardToken, request, userId);
        return ResponseEntity.ok(card);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get user cards", description = "Get all cards of the user")
    public ResponseEntity<Page<CardResponse>> getUserCards(
            Authentication authentication,
            Pageable pageable) {
        
        Long userId = extractUserId(authentication);
        Page<CardResponse> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardToken}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get card details")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable String cardToken,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.getCard(cardToken, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardToken}/block")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Block card", description = "Block card (report lost/stolen)")
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable String cardToken,
            @Valid @RequestBody CardBlockRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.blockCard(cardToken, request, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardToken}/freeze")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Freeze card", description = "Temporarily freeze card")
    public ResponseEntity<CardResponse> freezeCard(
            @PathVariable String cardToken,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.freezeCard(cardToken, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardToken}/unfreeze")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Unfreeze card")
    public ResponseEntity<CardResponse> unfreezeCard(
            @PathVariable String cardToken,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.unfreezeCard(cardToken, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardToken}/replace")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Replace card", description = "Issue replacement card")
    public ResponseEntity<CardResponse> replaceCard(
            @PathVariable String cardToken,
            @RequestParam String reason,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.replaceCard(cardToken, reason, userId);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/{cardToken}/pin")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Change PIN")
    public ResponseEntity<Void> changePin(
            @PathVariable String cardToken,
            @Valid @RequestBody PinChangeRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        cardService.changePin(cardToken, request, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cardToken}/limits")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update card limits")
    public ResponseEntity<CardResponse> updateLimits(
            @PathVariable String cardToken,
            @Valid @RequestBody CardLimitUpdateRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.updateLimits(cardToken, request, userId);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/{cardToken}/controls")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update card controls")
    public ResponseEntity<CardResponse> updateControls(
            @PathVariable String cardToken,
            @Valid @RequestBody CardControlsRequest request,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        CardResponse card = cardService.updateControls(cardToken, request, userId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/virtual")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Generate virtual card")
    public ResponseEntity<String> generateVirtualCard(
            @RequestParam Long accountId,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        String cardToken = cardService.generateVirtualCard(accountId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardToken);
    }

    @DeleteMapping("/{cardToken}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Close card")
    public ResponseEntity<Void> closeCard(
            @PathVariable String cardToken,
            Authentication authentication) {
        
        Long userId = extractUserId(authentication);
        cardService.closeCard(cardToken, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Card Service is healthy");
    }

    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
