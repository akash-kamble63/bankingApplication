package com.account_service.service;

import java.util.List;

import com.account_service.dto.BalanceSnapshotResponse;

public interface BalanceSnapshotService {
	 void createDailySnapshots();
	    void createMonthlySnapshots();
	    List<BalanceSnapshotResponse> getAccountSnapshots(Long accountId, String type);
	    BalanceSnapshotResponse getLatestSnapshot(Long accountId);
}
