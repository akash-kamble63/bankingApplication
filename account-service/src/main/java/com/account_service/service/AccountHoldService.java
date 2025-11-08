package com.account_service.service;

import java.math.BigDecimal;
import java.util.List;

import com.account_service.dto.AccountHoldResponse;
import com.account_service.dto.PlaceHoldRequest;

public interface AccountHoldService {
	AccountHoldResponse placeHold(PlaceHoldRequest request);
    AccountHoldResponse releaseHold(String holdReference);
    List<AccountHoldResponse> getAccountHolds(Long accountId);
    BigDecimal getTotalHolds(Long accountId);
    void expireOldHolds();
}
