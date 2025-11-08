package com.account_service.dto;
import java.time.LocalDateTime;

import com.account_service.enums.BeneficiaryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryResponse {
	private Long id;
    private Long userId;
    private Long accountId;
    private String beneficiaryName;
    private String beneficiaryAccountNumber;
    private String beneficiaryIfsc;
    private String beneficiaryBank;
    private String nickname;
    private BeneficiaryStatus status;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}
