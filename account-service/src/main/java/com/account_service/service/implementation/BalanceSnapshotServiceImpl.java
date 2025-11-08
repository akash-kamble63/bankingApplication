package com.account_service.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.account_service.dto.BalanceSnapshotResponse;
import com.account_service.enums.AccountStatus;
import com.account_service.model.Account;
import com.account_service.model.AccountBalanceSnapshot;
import com.account_service.repository.AccountBalanceSnapshotRepository;
import com.account_service.repository.AccountRepository;
import com.account_service.service.BalanceSnapshotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSnapshotServiceImpl implements BalanceSnapshotService {
    
    private final AccountRepository accountRepository;
    private final AccountBalanceSnapshotRepository snapshotRepository;
    
    @Override
    @Scheduled(cron = "${app.snapshot.schedule.daily:0 0 0 * * *}") // Midnight
    @Transactional
    public void createDailySnapshots() {
        log.info("Creating daily balance snapshots");
        LocalDateTime snapshotDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        createSnapshots("DAILY", snapshotDate);
    }
    
    @Override
    @Scheduled(cron = "${app.snapshot.schedule.monthly:0 0 0 1 * *}") // 1st of month
    @Transactional
    public void createMonthlySnapshots() {
        log.info("Creating monthly balance snapshots");
        LocalDateTime snapshotDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        createSnapshots("MONTHLY", snapshotDate);
    }
    
    private void createSnapshots(String type, LocalDateTime snapshotDate) {
        List<Account> activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE);
        
        for (Account account : activeAccounts) {
            // Check if snapshot already exists
            if (!snapshotRepository.existsByAccountIdAndSnapshotDateAndSnapshotType(
                    account.getId(), snapshotDate, type)) {
                
                AccountBalanceSnapshot snapshot = AccountBalanceSnapshot.builder()
                        .accountId(account.getId())
                        .balance(account.getBalance())
                        .availableBalance(account.getAvailableBalance())
                        .snapshotDate(snapshotDate)
                        .snapshotType(type)
                        .build();
                
                snapshotRepository.save(snapshot);
            }
        }
        
        log.info("Created {} snapshots for {} accounts", type, activeAccounts.size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BalanceSnapshotResponse> getAccountSnapshots(Long accountId, String type) {
        List<AccountBalanceSnapshot> snapshots = type != null
                ? snapshotRepository.findByAccountIdAndSnapshotType(accountId, type)
                : snapshotRepository.findByAccountId(accountId);
        
        return snapshots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public BalanceSnapshotResponse getLatestSnapshot(Long accountId) {
        return snapshotRepository.findLatestSnapshot(accountId)
                .map(this::mapToResponse)
                .orElse(null);
    }
    
    private BalanceSnapshotResponse mapToResponse(AccountBalanceSnapshot snapshot) {
        Account account = accountRepository.findById(snapshot.getAccountId()).orElse(null);
        
        return BalanceSnapshotResponse.builder()
                .id(snapshot.getId())
                .accountId(snapshot.getAccountId())
                .accountNumber(account != null ? account.getAccountNumber() : null)
                .balance(snapshot.getBalance())
                .availableBalance(snapshot.getAvailableBalance())
                .snapshotDate(snapshot.getSnapshotDate())
                .snapshotType(snapshot.getSnapshotType())
                .build();
    }
}