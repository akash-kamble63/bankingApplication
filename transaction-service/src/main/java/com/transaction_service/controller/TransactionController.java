package com.transaction_service.controller;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transaction_service.DTOs.ApiResponseDTO;
import com.transaction_service.DTOs.TransactionFilterRequest;
import com.transaction_service.DTOs.TransactionResponse;
import com.transaction_service.DTOs.TransactionSummaryResponse;
import com.transaction_service.DTOs.TransferRequest;
import com.transaction_service.exception.RateLimitExceededException;
import com.transaction_service.service.RateLimitService;
import com.transaction_service.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
	private final TransactionService transactionService;
	private final RateLimitService rateLimitService;

	/**
	 * Create transfer transaction ✅ Idempotent via header ✅ Rate limited ✅ Input
	 * validated
	 */
	@PostMapping("/transfer")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	@Operation(summary = "Create fund transfer")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Transfer created"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "409", description = "Duplicate request"),
			@ApiResponse(responseCode = "429", description = "Rate limit exceeded") })
	public ResponseEntity<ApiResponseDTO<TransactionResponse>> createTransfer(
			@Valid @RequestBody TransferRequest request,
			@RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
			Authentication authentication) {

		// Extract user ID from JWT
		Long userId = extractUserId(authentication);

		// Rate limiting
		if (!rateLimitService.checkLimit("transfer:" + userId, 10, Duration.ofMinutes(1))) {
			throw new RateLimitExceededException("Too many transfer requests");
		}

		// Set idempotency key
		request.setIdempotencyKey(idempotencyKey);

		// Execute transfer
		TransactionResponse response = transactionService.createTransfer(request, userId);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponseDTO.success(response, "Transfer initiated successfully"));
	}

	/**
	 * Get user transactions Paginated Cached No N+1 queries
	 */
	@GetMapping("/my-transactions")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<ApiResponseDTO<Page<TransactionResponse>>> getMyTransactions(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "DESC") String sortDir, Authentication authentication) {

		Long userId = extractUserId(authentication);

		Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, pageable);

		return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Transactions retrieved successfully"));
	}

	/**
	 * Get transaction by reference
	 */
	@GetMapping("/{transactionReference}")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<ApiResponseDTO<TransactionResponse>> getTransaction(@PathVariable String transactionReference,
			Authentication authentication) {

		TransactionResponse transaction = transactionService.getTransactionByReference(transactionReference);

		return ResponseEntity.ok(ApiResponseDTO.success(transaction, "Transaction retrieved"));
	}

	/**
	 * Get daily summary ✅ Aggregated query (no N+1)
	 */
	@GetMapping("/summary/daily")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<ApiResponseDTO<TransactionSummaryResponse>> getDailySummary(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Authentication authentication) {

		Long userId = extractUserId(authentication);

		TransactionSummaryResponse summary = transactionService.getUserSummary(userId, date);

		return ResponseEntity.ok(ApiResponseDTO.success(summary, "Summary retrieved"));
	}

	/**
	 * Cancel pending transaction
	 */
	@PutMapping("/{transactionReference}/cancel")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<ApiResponseDTO<Void>> cancelTransaction(@PathVariable String transactionReference,
			Authentication authentication) {

		Long userId = extractUserId(authentication);

		transactionService.cancelTransaction(transactionReference, userId);

		return ResponseEntity.ok(ApiResponseDTO.success("Transaction cancelled successfully"));
	}

	/**
	 * Admin: Get all transactions with filters
	 */
	@PostMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponseDTO<Page<TransactionResponse>>> searchTransactions(
			@Valid @RequestBody TransactionFilterRequest filter) {

		Page<TransactionResponse> transactions = transactionService.searchTransactions(filter);

		return ResponseEntity.ok(ApiResponseDTO.success(transactions, "Search completed"));
	}

	private Long extractUserId(Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		return Long.parseLong(jwt.getClaimAsString("user_id"));
	}
}
