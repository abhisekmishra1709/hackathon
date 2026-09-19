package com.sentinel.aml.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {
    private final CsvImportService csvImportService;

    public IngestionController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping(value = "/customers/csv", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResult importCustomers(@RequestParam("file") MultipartFile file) {
        return csvImportService.importCustomers(file);
    }

    @PostMapping(value = "/accounts/csv", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResult importAccounts(@RequestParam("file") MultipartFile file) {
        return csvImportService.importAccounts(file);
    }

    @PostMapping(value = "/transactions/csv", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResult importTransactions(@RequestParam("file") MultipartFile file) {
        return csvImportService.importTransactions(file);
    }
}