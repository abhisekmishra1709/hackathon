package com.sentinel.aml.casework;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public List<CaseResponse> list() {
        return caseService.list();
    }

    @PostMapping("/from-alert/{alertId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse create(@PathVariable("alertId") Long alertId, @Valid @RequestBody CaseRequest request,
            Principal principal) {
        return caseService.create(alertId, request, principal.getName());
    }

    @PatchMapping("/{id}")
    public CaseResponse update(@PathVariable("id") Long id, @Valid @RequestBody CaseUpdateRequest request,
            Principal principal) {
        return caseService.update(id, request, principal.getName());
    }
}