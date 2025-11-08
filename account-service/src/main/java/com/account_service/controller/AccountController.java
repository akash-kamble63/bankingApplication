package com.account_service.controller;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.account_service.annotation.Idempotent;
import com.account_service.dto.AccountEventResponse;
import com.account_service.dto.AccountFilterRequest;
import com.account_service.dto.AccountResponse;
import com.account_service.dto.AccountStatisticsResponse;
import com.account_service.dto.AccountSummaryResponse;
import com.account_service.dto.ApiResponseDTO;
import com.account_service.dto.BalanceResponse;
import com.account_service.dto.CreateAccountRequest;
import com.account_service.dto.UpdateAccountRequest;
import com.account_service.dto.UserAccountSummary;
import com.account_service.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {
	private final AccountService accountService;


    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Idempotent(ttlHours = 24)
    @Operation(summary = "Create new account", description = "Create a new bank account for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Account already exists")
    })
    public ResponseEntity<ApiResponseDTO<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {
        
        log.info("Create account request received for user: {}", request.getUserId());
        
        String createdBy = authentication.getName();
        AccountResponse account = accountService.createAccount(request, createdBy);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(account, "Account created successfully"));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account details", description = "Retrieve account details by account number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account found"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<AccountResponse>> getAccount(
            @PathVariable String accountNumber) {
        
        AccountResponse account = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Account retrieved successfully"));
    }

    @PutMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update account", description = "Update account details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account updated"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<AccountResponse>> updateAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication) {
        
        String updatedBy = authentication.getName();
        AccountResponse account = accountService.updateAccount(accountNumber, request, updatedBy);
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Account updated successfully"));
    }

    @DeleteMapping("/{accountNumber}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close account", description = "Permanently close an account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account closed"),
        @ApiResponse(responseCode = "400", description = "Cannot close account"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<Void>> closeAccount(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        String closedBy = authentication.getName();
        accountService.closeAccount(accountNumber, reason, closedBy);
        
        return ResponseEntity.ok(ApiResponseDTO.success("Account closed successfully"));
    }

 

    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account balance", description = "Retrieve current balance and available balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance retrieved"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<BalanceResponse>> getBalance(
            @PathVariable String accountNumber) {
        
        BalanceResponse balance = accountService.getBalance(accountNumber);
        return ResponseEntity.ok(ApiResponseDTO.success(balance, "Balance retrieved successfully"));
    }

    @PostMapping("/{accountNumber}/credit")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent(ttlHours = 1)
    @Operation(summary = "Credit account", description = "Add funds to account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account credited"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<BalanceResponse>> creditAccount(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            @RequestParam(required = false) String transactionRef) {
        
        BalanceResponse balance = accountService.creditAccount(accountNumber, amount, reason, transactionRef);
        return ResponseEntity.ok(ApiResponseDTO.success(balance, "Account credited successfully"));
    }

    @PostMapping("/{accountNumber}/debit")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent(ttlHours = 1)
    @Operation(summary = "Debit account", description = "Deduct funds from account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account debited"),
        @ApiResponse(responseCode = "400", description = "Insufficient balance"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<BalanceResponse>> debitAccount(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            @RequestParam(required = false) String transactionRef) {
        
        BalanceResponse balance = accountService.debitAccount(accountNumber, amount, reason, transactionRef);
        return ResponseEntity.ok(ApiResponseDTO.success(balance, "Account debited successfully"));
    }


    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get user accounts", description = "Retrieve all accounts for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Accounts retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponseDTO<List<AccountSummaryResponse>>> getUserAccounts(
            @PathVariable Long userId) {
        
        List<AccountSummaryResponse> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(ApiResponseDTO.success(accounts, "User accounts retrieved successfully"));
    }

    @GetMapping("/user/{userId}/primary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get primary account", description = "Get user's primary account")
    public ResponseEntity<ApiResponseDTO<AccountResponse>> getPrimaryAccount(
            @PathVariable Long userId) {
        
        AccountResponse account = accountService.getPrimaryAccount(userId);
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Primary account retrieved"));
    }



    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search accounts", description = "Search accounts with filters")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<ApiResponseDTO<Page<AccountResponse>>> searchAccounts(
            @Valid @RequestBody AccountFilterRequest filter) {
        
        Pageable pageable = PageRequest.of(
            filter.getPage(), 
            filter.getSize(),
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy())
        );
        
        Page<AccountResponse> accounts = accountService.filterAccounts(filter, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(accounts, "Search completed"));
    }



    @PutMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Freeze account", description = "Temporarily freeze an account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account frozen"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponseDTO<AccountResponse>> freezeAccount(
            @PathVariable String accountNumber,
            @RequestParam String reason,
            Authentication authentication) {
        
        String updatedBy = authentication.getName();
        AccountResponse account = accountService.freezeAccount(accountNumber, reason, updatedBy);
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Account frozen successfully"));
    }

    @PutMapping("/{accountNumber}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unfreeze account", description = "Unfreeze a frozen account")
    public ResponseEntity<ApiResponseDTO<AccountResponse>> unfreezeAccount(
            @PathVariable String accountNumber,
            Authentication authentication) {
        
        String updatedBy = authentication.getName();
        AccountResponse account = accountService.unfreezeAccount(accountNumber, updatedBy);
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Account unfrozen successfully"));
    }

    @PutMapping("/{accountNumber}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate account", description = "Activate inactive/dormant account")
    public ResponseEntity<ApiResponseDTO<AccountResponse>> activateAccount(
            @PathVariable String accountNumber,
            Authentication authentication) {
        
        String updatedBy = authentication.getName();
        AccountResponse account = accountService.activateAccount(accountNumber, updatedBy);
        
        return ResponseEntity.ok(ApiResponseDTO.success(account, "Account activated successfully"));
    }


    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account statistics", description = "Retrieve account statistics and metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    })
    public ResponseEntity<ApiResponseDTO<AccountStatisticsResponse>> getStatistics() {
        AccountStatisticsResponse stats = accountService.getStatistics();
        return ResponseEntity.ok(ApiResponseDTO.success(stats, "Statistics retrieved successfully"));
    }

    @GetMapping("/user/{userId}/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get user account summary", description = "Get summary of all user accounts")
    public ResponseEntity<ApiResponseDTO<UserAccountSummary>> getUserSummary(
            @PathVariable Long userId) {
        
        UserAccountSummary summary = accountService.getUserAccountSummary(userId);
        return ResponseEntity.ok(ApiResponseDTO.success(summary, "User summary retrieved"));
    }



    @GetMapping("/{accountNumber}/events")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account events", description = "Retrieve event history for account")
    public ResponseEntity<ApiResponseDTO<List<AccountEventResponse>>> getAccountEvents(
            @PathVariable String accountNumber,
            @RequestParam(required = false) Long fromVersion) {
        
        List<AccountEventResponse> events = accountService.getAccountEvents(accountNumber, fromVersion);
        return ResponseEntity.ok(ApiResponseDTO.success(events, "Events retrieved successfully"));
    }

    @GetMapping("/{accountNumber}/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get account snapshot", description = "Get account state at specific point in time")
    public ResponseEntity<ApiResponseDTO<AccountResponse>> getAccountSnapshot(
            @PathVariable String accountNumber,
            @Parameter(description = "Point in time (ISO format)") @RequestParam String pointInTime) {
        
        AccountResponse snapshot = accountService.getAccountSnapshot(accountNumber, pointInTime);
        return ResponseEntity.ok(ApiResponseDTO.success(snapshot, "Snapshot retrieved"));
    }



    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check service health")
    public ResponseEntity<ApiResponseDTO<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponseDTO.success("Account service is healthy", "OK"));
    }
}
