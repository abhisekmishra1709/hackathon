package com.sentinel.aml.detection;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;

import com.sentinel.aml.audit.AuditEvent;
import com.sentinel.aml.audit.AuditEventRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleConfigurationController {
    private final AmlRuleProperties properties;
    private final AuditEventRepository auditRepository;

    public RuleConfigurationController(AmlRuleProperties properties, AuditEventRepository auditRepository) {
        this.properties = properties;
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public RuleConfiguration get() {
        return RuleConfiguration.from(properties);
    }

    @PutMapping
    public synchronized RuleConfiguration update(@Valid @RequestBody RuleConfiguration request, Principal principal) {
        if (request.structuringMinimumInr().compareTo(request.reportThresholdInr()) >= 0) {
            throw new IllegalArgumentException("structuringMinimumInr must be below reportThresholdInr");
        }
        properties.setThresholdEnabled(request.thresholdEnabled());
        properties.setStructuringEnabled(request.structuringEnabled());
        properties.setRapidMovementEnabled(request.rapidMovementEnabled());
        properties.setHighRiskEnabled(request.highRiskEnabled());
        properties.setBehavioralDeviationEnabled(request.behavioralDeviationEnabled());
        properties.setRoundNumberEnabled(request.roundNumberEnabled());
        properties.setReportThresholdInr(request.reportThresholdInr());
        properties.setStructuringMinimumInr(request.structuringMinimumInr());
        properties.setStructuringCount(request.structuringCount());
        properties.setStructuringWindowHours(request.structuringWindowHours());
        properties.setRapidMovementHours(request.rapidMovementHours());
        properties.setRapidMovementRatio(request.rapidMovementRatio());
        properties.setRoundAmountUnitInr(request.roundAmountUnitInr());
        properties.setHighRiskJurisdictions(new HashSet<>(request.highRiskJurisdictions()));
        properties.setHighRiskCounterparties(new HashSet<>(request.highRiskCounterparties()));
        properties.setExchangeRatesToInr(new HashMap<>(request.exchangeRatesToInr()));
        auditRepository.save(new AuditEvent("RULE_CONFIGURATION", 0L, "UPDATED", principal.getName(),
                "Runtime AML rule configuration updated"));
        return RuleConfiguration.from(properties);
    }
}