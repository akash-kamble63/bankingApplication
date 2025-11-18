package com.card_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardControlsRequest {
	private Boolean contactlessEnabled;
    private Boolean onlineTransactionsEnabled;
    private Boolean internationalTransactionsEnabled;
    private Boolean atmEnabled;
    private Boolean posEnabled;
}
