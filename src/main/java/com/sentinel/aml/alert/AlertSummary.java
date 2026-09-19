package com.sentinel.aml.alert;

import java.time.Instant;

public record AlertSummary(Long id, String customer, String accountId, String rules, int riskScore,
        String status, int evidenceCount, Instant createdAt) {

    static AlertSummary from(Alert alert) {
        String name = alert.getCustomer().getFullName();
        String maskedName = name.isBlank() ? "***" : name.charAt(0) + "***";
        String accountId = alert.getAccount().getExternalId();
        String maskedAccount = "***" + accountId.substring(Math.max(0, accountId.length() - 4));
        return new AlertSummary(alert.getId(), maskedName, maskedAccount, alert.getRuleCode(),
                alert.getRiskScore(), alert.getStatus(), alert.getEvidence().size(), alert.getCreatedAt());
    }
}