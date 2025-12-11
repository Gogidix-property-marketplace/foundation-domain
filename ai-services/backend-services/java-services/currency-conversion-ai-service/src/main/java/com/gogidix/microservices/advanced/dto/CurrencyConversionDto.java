package com.gogidix.microservices.advanced.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for Currency Conversion AI Service - Streamlined for rapid implementation
 */

// Request DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionRequestDto {
    @NotBlank
    private String fromCurrency;

    @NotBlank
    private String toCurrency;

    @NotNull
    @Min(0.01)
    private Double amount;

    private String conversionType;
    private String timingPreference;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMarketAdaptationRequestDto {
    @NotBlank
    private String targetCountry;

    private String businessType;
    private List<String> investmentGoals;
    private Map<String, Object> financialParameters;
    private String timeHorizon;
}

// Result DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionDto {
    private String conversionId;
    private String fromCurrency;
    private String toCurrency;
    private Double amount;
    private Double exchangeRate;
    private Double convertedAmount;
    private Double aiConfidence;
    private String marketVolatility;
    private String recommendedTiming;
    private String predictedTrend;
    private LocalDateTime conversionDate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMarketAdaptationDto {
    private String adaptationId;
    private String targetCountry;
    private String currencyCode;
    private Map<String, Object> localMarketConditions;
    private List<String> regulatoryRequirements;
    private Map<String, Object> taxStructure;
    private Map<String, Object> investmentClimate;
    private String marketRisk;
    private Double adaptationScore;
    private List<String> recommendations;
    private LocalDateTime adaptationDate;
}