package com.sentinel.aml.detection;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinel.rules")
public class AmlRuleProperties {
    private boolean thresholdEnabled = true;
    private boolean structuringEnabled = true;
    private boolean rapidMovementEnabled = true;
    private boolean highRiskEnabled = true;
    private boolean behavioralDeviationEnabled = true;
    private boolean roundNumberEnabled = true;
    private BigDecimal reportThresholdInr = new BigDecimal("830000");
    private BigDecimal structuringMinimumInr = new BigDecimal("747000");
    private int structuringCount = 3;
    private int structuringWindowHours = 24;
    private int rapidMovementHours = 48;
    private BigDecimal rapidMovementRatio = new BigDecimal("0.80");
    private BigDecimal roundAmountUnitInr = new BigDecimal("100000");
    private Set<String> highRiskJurisdictions = new HashSet<>(Set.of("IR", "KP", "SY"));
    private Set<String> highRiskCounterparties = new HashSet<>();
        public boolean isThresholdEnabled() { return thresholdEnabled; }
        public void setThresholdEnabled(boolean value) { thresholdEnabled = value; }
        public boolean isStructuringEnabled() { return structuringEnabled; }
        public void setStructuringEnabled(boolean value) { structuringEnabled = value; }
        public boolean isRapidMovementEnabled() { return rapidMovementEnabled; }
        public void setRapidMovementEnabled(boolean value) { rapidMovementEnabled = value; }
        public boolean isHighRiskEnabled() { return highRiskEnabled; }
        public void setHighRiskEnabled(boolean value) { highRiskEnabled = value; }
        public boolean isBehavioralDeviationEnabled() { return behavioralDeviationEnabled; }
        public void setBehavioralDeviationEnabled(boolean value) { behavioralDeviationEnabled = value; }
        public boolean isRoundNumberEnabled() { return roundNumberEnabled; }
        public void setRoundNumberEnabled(boolean value) { roundNumberEnabled = value; }
    private Map<String, BigDecimal> exchangeRatesToInr = new HashMap<>(Map.of(
            "INR", BigDecimal.ONE, "USD", new BigDecimal("83"), "EUR", new BigDecimal("90")));

    public BigDecimal getReportThresholdInr() { return reportThresholdInr; }
    public void setReportThresholdInr(BigDecimal value) { reportThresholdInr = value; }
    public BigDecimal getStructuringMinimumInr() { return structuringMinimumInr; }
    public void setStructuringMinimumInr(BigDecimal value) { structuringMinimumInr = value; }
    public int getStructuringCount() { return structuringCount; }
    public void setStructuringCount(int value) { structuringCount = value; }
    public int getStructuringWindowHours() { return structuringWindowHours; }
    public void setStructuringWindowHours(int value) { structuringWindowHours = value; }
    public int getRapidMovementHours() { return rapidMovementHours; }
    public void setRapidMovementHours(int value) { rapidMovementHours = value; }
    public BigDecimal getRapidMovementRatio() { return rapidMovementRatio; }
    public void setRapidMovementRatio(BigDecimal value) { rapidMovementRatio = value; }
    public BigDecimal getRoundAmountUnitInr() { return roundAmountUnitInr; }
    public void setRoundAmountUnitInr(BigDecimal value) { roundAmountUnitInr = value; }
    public Set<String> getHighRiskJurisdictions() { return highRiskJurisdictions; }
    public void setHighRiskJurisdictions(Set<String> value) { highRiskJurisdictions = value; }
    public Set<String> getHighRiskCounterparties() { return highRiskCounterparties; }
    public void setHighRiskCounterparties(Set<String> value) { highRiskCounterparties = value; }
    public Map<String, BigDecimal> getExchangeRatesToInr() { return exchangeRatesToInr; }
    public void setExchangeRatesToInr(Map<String, BigDecimal> value) { exchangeRatesToInr = value; }
}