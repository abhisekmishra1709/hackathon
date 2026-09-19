package com.sentinel.aml.detection;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RuleConfiguration(
        @NotNull Boolean thresholdEnabled,
        @NotNull Boolean structuringEnabled,
        @NotNull Boolean rapidMovementEnabled,
        @NotNull Boolean highRiskEnabled,
        @NotNull Boolean behavioralDeviationEnabled,
        @NotNull Boolean roundNumberEnabled,
        @NotNull @DecimalMin("0.01") BigDecimal reportThresholdInr,
        @NotNull @DecimalMin("0.01") BigDecimal structuringMinimumInr,
        @Min(2) int structuringCount,
        @Min(1) int structuringWindowHours,
        @Min(1) int rapidMovementHours,
        @NotNull @DecimalMin("0.01") @DecimalMax("1.0") BigDecimal rapidMovementRatio,
        @NotNull @DecimalMin("0.01") BigDecimal roundAmountUnitInr,
        @NotNull Set<String> highRiskJurisdictions,
        @NotNull Set<String> highRiskCounterparties,
        @NotEmpty Map<String, @DecimalMin("0.0001") BigDecimal> exchangeRatesToInr) {

    static RuleConfiguration from(AmlRuleProperties properties) {
        return new RuleConfiguration(properties.isThresholdEnabled(), properties.isStructuringEnabled(),
                properties.isRapidMovementEnabled(), properties.isHighRiskEnabled(),
                properties.isBehavioralDeviationEnabled(), properties.isRoundNumberEnabled(),
                properties.getReportThresholdInr(), properties.getStructuringMinimumInr(),
                properties.getStructuringCount(), properties.getStructuringWindowHours(),
                properties.getRapidMovementHours(), properties.getRapidMovementRatio(),
                properties.getRoundAmountUnitInr(), Set.copyOf(properties.getHighRiskJurisdictions()),
                Set.copyOf(properties.getHighRiskCounterparties()), Map.copyOf(properties.getExchangeRatesToInr()));
    }
}