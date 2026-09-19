package com.sentinel.aml.alert;

import java.time.Instant;
import java.util.List;

public record AlertDetail(Long id, String customerId, String customerName, String accountId, String rules,
        int riskScore, String explanation, String status, String assignedTo, String dispositionReason,
        List<String> transactionIds, Instant createdAt) {

    static AlertDetail from(Alert alert) {
        List<String> evidence = alert.getEvidence().stream().map(transaction -> transaction.getExternalId()).toList();
        return new AlertDetail(alert.getId(), alert.getCustomer().getExternalId(), alert.getCustomer().getFullName(),
                alert.getAccount().getExternalId(), alert.getRuleCode(), alert.getRiskScore(), alert.getExplanation(),
                alert.getStatus(), alert.getAssignedTo(), alert.getDispositionReason(), evidence, alert.getCreatedAt());
    }
}