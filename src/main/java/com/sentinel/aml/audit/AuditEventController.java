package com.sentinel.aml.audit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventController {
    private final AuditEventRepository auditEventRepository;

    public AuditEventController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    public List<AuditEventResponse> list() {
        return auditEventRepository.findAllByOrderByOccurredAtDesc().stream().map(AuditEventResponse::from).toList();
    }
}