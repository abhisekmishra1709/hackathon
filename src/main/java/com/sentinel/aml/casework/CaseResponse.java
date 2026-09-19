package com.sentinel.aml.casework;

import java.time.Instant;

public record CaseResponse(Long id, Long alertId, String status, String analystId, String notes,
        String disposition, Instant createdAt) {
    static CaseResponse from(InvestigationCase investigationCase) {
        return new CaseResponse(investigationCase.getId(), investigationCase.getAlert().getId(),
                investigationCase.getStatus(), investigationCase.getAnalystId(), investigationCase.getNotes(),
                investigationCase.getDisposition(), investigationCase.getCreatedAt());
    }
}