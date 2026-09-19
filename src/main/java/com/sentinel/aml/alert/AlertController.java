package com.sentinel.aml.alert;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sentinel.aml.transaction.Transaction;
import com.sentinel.aml.transaction.TransactionRepository;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    public AlertController(AlertRepository alertRepository, TransactionRepository transactionRepository) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<AlertSummary> list() {
        return alertRepository.findAllByOrderByRiskScoreDescCreatedAtDesc().stream().map(AlertSummary::from).toList();
    }

    @GetMapping("/{id}")
    public AlertDetail detail(@PathVariable("id") Long id) {
        Alert alert = alertRepository.findWithEvidenceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        return AlertDetail.from(alert);
    }

    @GetMapping("/{id}/timeline")
    public List<TransactionTimelineEntry> timeline(@PathVariable("id") Long id) {
        Alert alert = alertRepository.findWithEvidenceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        Set<String> evidenceIds = alert.getEvidence().stream().map(Transaction::getExternalId)
                .collect(Collectors.toSet());
        return transactionRepository.findByAccount_ExternalIdOrderByOccurredAt(alert.getAccount().getExternalId())
                .stream()
                .map(transaction -> TransactionTimelineEntry.from(transaction, evidenceIds.contains(transaction.getExternalId())))
                .toList();
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    void deletionIsNotSupported() {
    }
}