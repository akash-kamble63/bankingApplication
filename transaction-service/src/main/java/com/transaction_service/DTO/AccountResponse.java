package com.transaction_service.DTO;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private Long userId;
    private String userEmail;
    private BigDecimal balance;
	private BigDecimal availableBalance;
	private String status;
}
