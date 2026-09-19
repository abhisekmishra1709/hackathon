package com.sentinel.aml.alert;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
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

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    void deletionIsNotSupported() {
    }
}