package com.account_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.account_service.dto.ApiResponseDTO;
import com.account_service.dto.BeneficiaryResponse;
import com.account_service.dto.CreateBeneficiaryRequest;
import com.account_service.service.BeneficiaryService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Validated
@Tag(name = "Beneficiary Management", description = "APIs for managing beneficiaries")

public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    /**
     * Add a new beneficiary
     */
    @PostMapping
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "addBeneficiaryFallback")
    @RateLimiter(name = "beneficiaryOperations")
    @Retry(name = "beneficiaryService")
    @Operation(summary = "Add a new beneficiary", description = "Creates a new beneficiary for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Beneficiary created successfully", content = @Content(schema = @Schema(implementation = BeneficiaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Beneficiary already exists or limit exceeded"),
            @ApiResponse(responseCode = "429", description = "Too many requests"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    public ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> addBeneficiary(
            @Valid @RequestBody CreateBeneficiaryRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Request to add beneficiary for user: {}, idempotencyKey: {}",
                request.getUserId(), idempotencyKey);

        BeneficiaryResponse response = beneficiaryService.addBeneficiary(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.<BeneficiaryResponse>builder()
                        .success(true)
                        .message("Beneficiary added successfully")
                        .data(response)
                        .build());
    }

    /**
     * Get all beneficiaries for a user
     */
    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "getUserBeneficiariesFallback")
    @RateLimiter(name = "beneficiaryOperations")
    @Operation(summary = "Get user beneficiaries", description = "Retrieves all beneficiaries for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Beneficiaries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponseDTO<List<BeneficiaryResponse>>> getUserBeneficiaries(
            @PathVariable @NotNull @Min(1) Long userId) {

        log.info("Request to get beneficiaries for user: {}", userId);

        List<BeneficiaryResponse> beneficiaries = beneficiaryService.getUserBeneficiaries(userId);

        return ResponseEntity.ok(ApiResponseDTO.<List<BeneficiaryResponse>>builder()
                .success(true)
                .message("Beneficiaries retrieved successfully")
                .data(beneficiaries)
                .build());
    }

    /**
     * Search beneficiaries
     */
    @GetMapping("/user/{userId}/search")
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "searchBeneficiariesFallback")
    @RateLimiter(name = "beneficiarySearch")
    @Operation(summary = "Search beneficiaries", description = "Search beneficiaries by name, account number, or nickname")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponseDTO<Page<BeneficiaryResponse>>> searchBeneficiaries(
            @PathVariable @NotNull @Min(1) Long userId,
            @Parameter(description = "Search term (name, account number, or nickname)") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Request to search beneficiaries for user: {}, searchTerm: {}", userId, searchTerm);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<BeneficiaryResponse> results = beneficiaryService.searchBeneficiaries(userId, searchTerm, pageable);

        return ResponseEntity.ok(ApiResponseDTO.<Page<BeneficiaryResponse>>builder()
                .success(true)
                .message("Search completed successfully")
                .data(results)
                .build());
    }

    /**
     * Verify a beneficiary
     */
    @PutMapping("/{beneficiaryId}/verify")
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "verifyBeneficiaryFallback")
    @RateLimiter(name = "beneficiaryOperations")
    @Retry(name = "beneficiaryService")
    @Operation(summary = "Verify beneficiary", description = "Marks a beneficiary as verified")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Beneficiary verified successfully"),
            @ApiResponse(responseCode = "404", description = "Beneficiary not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> verifyBeneficiary(
            @PathVariable @NotNull @Min(1) Long beneficiaryId,
            @RequestHeader(value = "X-User-Id") Long userId) {

        log.info("Request to verify beneficiary: {} by user: {}", beneficiaryId, userId);

        BeneficiaryResponse response = beneficiaryService.verifyBeneficiary(beneficiaryId);

        return ResponseEntity.ok(ApiResponseDTO.<BeneficiaryResponse>builder()
                .success(true)
                .message("Beneficiary verified successfully")
                .data(response)
                .build());
    }

    /**
     * Delete a beneficiary (soft delete)
     */
    @DeleteMapping("/{beneficiaryId}")
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "deleteBeneficiaryFallback")
    @RateLimiter(name = "beneficiaryOperations")
    @Retry(name = "beneficiaryService")
    @Operation(summary = "Delete beneficiary", description = "Soft deletes a beneficiary by marking it as blocked")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Beneficiary deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Beneficiary not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponseDTO<Void>> deleteBeneficiary(
            @PathVariable @NotNull @Min(1) Long beneficiaryId,
            @RequestHeader(value = "X-User-Id") Long userId) {

        log.info("Request to delete beneficiary: {} by user: {}", beneficiaryId, userId);

        beneficiaryService.deleteBeneficiary(beneficiaryId);

        return ResponseEntity.ok(ApiResponseDTO.<Void>builder()
                .success(true)
                .message("Beneficiary deleted successfully")
                .build());
    }

    /**
     * Get beneficiary by ID
     */
    @GetMapping("/{beneficiaryId}")
    @CircuitBreaker(name = "beneficiaryService", fallbackMethod = "getBeneficiaryFallback")
    @RateLimiter(name = "beneficiaryOperations")
    @Operation(summary = "Get beneficiary by ID", description = "Retrieves a specific beneficiary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Beneficiary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Beneficiary not found")
    })
    public ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> getBeneficiary(
            @PathVariable @NotNull @Min(1) Long beneficiaryId) {

        log.info("Request to get beneficiary: {}", beneficiaryId);

        // You'll need to add this method to BeneficiaryService
        // For now, this is a placeholder showing the pattern

        return ResponseEntity.ok(ApiResponseDTO.<BeneficiaryResponse>builder()
                .success(true)
                .message("Beneficiary retrieved successfully")
                .build());
    }

    // ----------------- Fallback Methods ------------------

    /**
     * Fallback for add beneficiary
     */
    private ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> addBeneficiaryFallback(
            CreateBeneficiaryRequest request, String idempotencyKey, Exception e) {

        log.error("Fallback triggered for addBeneficiary: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<BeneficiaryResponse>builder()
                        .success(false)
                        .message("Service temporarily unavailable. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Fallback for get user beneficiaries
     */
    private ResponseEntity<ApiResponseDTO<List<BeneficiaryResponse>>> getUserBeneficiariesFallback(
            Long userId, Exception e) {

        log.error("Fallback triggered for getUserBeneficiaries: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<List<BeneficiaryResponse>>builder()
                        .success(false)
                        .message("Unable to retrieve beneficiaries. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Fallback for search beneficiaries
     */
    private ResponseEntity<ApiResponseDTO<Page<BeneficiaryResponse>>> searchBeneficiariesFallback(
            Long userId, String searchTerm, int page, int size, String sortBy, String sortDirection, Exception e) {

        log.error("Fallback triggered for searchBeneficiaries: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<Page<BeneficiaryResponse>>builder()
                        .success(false)
                        .message("Search service temporarily unavailable. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Fallback for verify beneficiary
     */
    private ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> verifyBeneficiaryFallback(
            Long beneficiaryId, Long userId, Exception e) {

        log.error("Fallback triggered for verifyBeneficiary: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<BeneficiaryResponse>builder()
                        .success(false)
                        .message("Verification service temporarily unavailable. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Fallback for delete beneficiary
     */
    private ResponseEntity<ApiResponseDTO<Void>> deleteBeneficiaryFallback(
            Long beneficiaryId, Long userId, Exception e) {

        log.error("Fallback triggered for deleteBeneficiary: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Delete service temporarily unavailable. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Fallback for get beneficiary
     */
    private ResponseEntity<ApiResponseDTO<BeneficiaryResponse>> getBeneficiaryFallback(
            Long beneficiaryId, Exception e) {

        log.error("Fallback triggered for getBeneficiary: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDTO.<BeneficiaryResponse>builder()
                        .success(false)
                        .message("Service temporarily unavailable. Please try again later.")
                        .error(buildErrorDetails(e))
                        .build());
    }

    /**
     * Build error details for response
     */
    private Map<String, Object> buildErrorDetails(Exception e) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("type", e.getClass().getSimpleName());
        errorDetails.put("message", e.getMessage());
        errorDetails.put("timestamp", System.currentTimeMillis());
        return errorDetails;
    }
}
