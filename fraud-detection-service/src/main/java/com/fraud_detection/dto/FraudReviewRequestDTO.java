package com.fraud_detection.dto;

import com.fraud_detection.enums.ReviewDecision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudReviewRequestDTO {
	@NotNull(message = "Decision is required")
    private ReviewDecision decision;
    
    @NotBlank(message = "Review notes are required")
    private String notes;
}
