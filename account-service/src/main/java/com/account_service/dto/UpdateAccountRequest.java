package com.account_service.dto;

import com.account_service.enums.AccountStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAccountRequest {
	private AccountStatus status;
    private Boolean isPrimary;
    private String branchCode;
}
