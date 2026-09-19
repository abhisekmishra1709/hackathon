package com.sentinel.aml.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.account.AccountRepository;
import com.sentinel.aml.customer.Customer;
import com.sentinel.aml.customer.CustomerRepository;
import com.sentinel.aml.transaction.TransactionIngestionService;
import com.sentinel.aml.transaction.TransactionRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionIngestionService transactionIngestionService;

    public CsvImportService(CustomerRepository customerRepository, AccountRepository accountRepository,
            TransactionIngestionService transactionIngestionService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionIngestionService = transactionIngestionService;
    }

    public ImportResult importCustomers(MultipartFile file) {
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (CSVRecord row : parse(file)) {
            try {
                String externalId = required(row, "customer_id");
                Customer customer = customerRepository.findByExternalId(externalId).orElseGet(() -> new Customer(externalId));
                boolean isNew = customer.getId() == null;
                customer.update(
                        required(row, "first_name"), required(row, "last_name"), date(row, "date_of_birth"),
                        value(row, "email"), value(row, "phone_number"), value(row, "city"), value(row, "state"),
                        required(row, "country"), decimal(row, "annual_income"), value(row, "customer_segment"),
                        value(row, "kyc_status"), required(row, "risk_rating"), "1".equals(value(row, "is_politically_exposed")));
                customerRepository.save(customer);
                if (isNew) created++; else updated++;
            } catch (RuntimeException exception) {
                log.warn("Rejected customer CSV row {}: {}", row.getRecordNumber(), exception.getMessage());
                errors.add("Row " + row.getRecordNumber() + ": " + exception.getMessage());
            }
        }
        ImportResult result = new ImportResult(created + updated + errors.size(), created, updated, errors);
        log.info("Customer CSV import completed: created={}, updated={}, rejected={}",
            created, updated, errors.size());
        return result;
    }

    public ImportResult importAccounts(MultipartFile file) {
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (CSVRecord row : parse(file)) {
            try {
                String externalId = required(row, "account_id");
                String customerId = required(row, "customer_id");
                Customer customer = customerRepository.findByExternalId(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown customer_id " + customerId));
                Account account = accountRepository.findByExternalId(externalId).orElseGet(() -> new Account(externalId));
                boolean isNew = account.getId() == null;
                account.update(customer, required(row, "account_type"), required(row, "account_status"),
                        required(row, "currency"), customer.getRiskRating(), date(row, "open_date"),
                        decimal(row, "current_balance"), value(row, "branch_code"), value(row, "account_tier"));
                accountRepository.save(account);
                if (isNew) created++; else updated++;
            } catch (RuntimeException exception) {
                log.warn("Rejected account CSV row {}: {}", row.getRecordNumber(), exception.getMessage());
                errors.add("Row " + row.getRecordNumber() + ": " + exception.getMessage());
            }
        }
        ImportResult result = new ImportResult(created + updated + errors.size(), created, updated, errors);
        log.info("Account CSV import completed: created={}, updated={}, rejected={}",
            created, updated, errors.size());
        return result;
    }

    public ImportResult importTransactions(MultipartFile file) {
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (CSVRecord row : parse(file)) {
            try {
                boolean wasCreated = transactionIngestionService.ingest(new TransactionRequest(
                        required(row, "transaction_id"), required(row, "account_id"),
                        required(row, "transaction_type"), decimal(row, "amount"), required(row, "currency"),
                        value(row, "counterparty"), required(row, "channel"),
                        java.time.Instant.parse(required(row, "timestamp")), required(row, "jurisdiction"))).created();
                if (wasCreated) created++; else updated++;
            } catch (RuntimeException exception) {
                log.warn("Rejected transaction CSV row {}: {}", row.getRecordNumber(), exception.getMessage());
                errors.add("Row " + row.getRecordNumber() + ": " + exception.getMessage());
            }
        }
        ImportResult result = new ImportResult(created + updated + errors.size(), created, updated, errors);
        log.info("Transaction CSV import completed: created={}, existing={}, rejected={}",
            created, updated, errors.size());
        return result;
    }

    private List<CSVRecord> parse(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            return parser.getRecords();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read CSV file", exception);
        }
    }

    private String required(CSVRecord row, String header) {
        String result = value(row, header);
        if (result.isBlank()) throw new IllegalArgumentException(header + " is required");
        return result;
    }

    private String value(CSVRecord row, String header) {
        if (!row.isMapped(header)) throw new IllegalArgumentException("Missing header " + header);
        return row.get(header).trim();
    }

    private LocalDate date(CSVRecord row, String header) {
        return LocalDate.parse(required(row, header));
    }

    private BigDecimal decimal(CSVRecord row, String header) {
        String value = value(row, header);
        return value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}