package com.sentinel.aml.casework;

import java.util.List;

import com.sentinel.aml.alert.Alert;
import com.sentinel.aml.alert.AlertRepository;
import com.sentinel.aml.audit.AuditEvent;
import com.sentinel.aml.audit.AuditEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaseService {
    private final InvestigationCaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final AuditEventRepository auditRepository;

    public CaseService(InvestigationCaseRepository caseRepository, AlertRepository alertRepository,
            AuditEventRepository auditRepository) {
        this.caseRepository = caseRepository;
        this.alertRepository = alertRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public CaseResponse create(Long alertId, CaseRequest request, String actor) {
        if (caseRepository.findByAlertId(alertId).isPresent()) {
            throw new IllegalArgumentException("A case already exists for alert " + alertId);
        }
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        alert.assign(actor);
        InvestigationCase investigationCase = caseRepository.save(
            new InvestigationCase(alert, actor, request.notes()));
        auditRepository.save(new AuditEvent("CASE", investigationCase.getId(), "CREATED", actor,
                "Created from alert " + alertId));
        auditRepository.save(new AuditEvent("ALERT", alertId, "STATUS_CHANGED", actor,
            "Status changed to INVESTIGATING"));
        return CaseResponse.from(investigationCase);
    }

    @Transactional
    public CaseResponse update(Long id, CaseUpdateRequest request, String actor) {
        InvestigationCase investigationCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        String status = request.status().toUpperCase();
        if (!List.of("OPEN", "INVESTIGATING", "CLOSED").contains(status)) {
            throw new IllegalArgumentException("status must be OPEN, INVESTIGATING, or CLOSED");
        }
        if ("CLOSED".equals(status) && (request.disposition() == null || request.disposition().isBlank())) {
            throw new IllegalArgumentException("disposition is required when closing a case");
        }
        investigationCase.update(status, request.notes(), request.disposition());
        if ("CLOSED".equals(status)) {
            investigationCase.getAlert().close(actor, request.disposition());
            auditRepository.save(new AuditEvent("ALERT", investigationCase.getAlert().getId(), "STATUS_CHANGED",
                    actor, "Status changed to CLOSED; disposition=" + request.disposition()));
        }
        auditRepository.save(new AuditEvent("CASE", id, "STATUS_CHANGED", actor,
                "Status changed to " + status + "; disposition=" + request.disposition()));
        return CaseResponse.from(investigationCase);
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> list() {
        return caseRepository.findAllByOrderByCreatedAtDesc().stream().map(CaseResponse::from).toList();
    }
}