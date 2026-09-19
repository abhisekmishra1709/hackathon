package com.sentinel.aml.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.account.AccountRepository;
import com.sentinel.aml.alert.Alert;
import com.sentinel.aml.detection.AmlRuleProperties;
import com.sentinel.aml.detection.DetectionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionIngestionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionIngestionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final DetectionEngine detectionEngine;
    private final AmlRuleProperties properties;

    public TransactionIngestionService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            DetectionEngine detectionEngine, AmlRuleProperties properties) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.detectionEngine = detectionEngine;
        this.properties = properties;
    }

    @Transactional
    public TransactionResult ingest(TransactionRequest request) {
        Account account = accountRepository.findByExternalIdForUpdate(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown account_id " + request.accountId()));
        Transaction existing = transactionRepository.findByExternalId(request.transactionId()).orElse(null);
        if (existing != null) {
            log.info("Skipped duplicate transaction {}", request.transactionId());
            return new TransactionResult(existing.getExternalId(), null, 0, false);
        }
        String currency = request.currency().toUpperCase();
        BigDecimal rate = properties.getExchangeRatesToInr().get(currency);
        if (rate == null) throw new IllegalArgumentException("No INR exchange rate configured for " + currency);
        BigDecimal amountInr = request.amount().multiply(rate).setScale(4, RoundingMode.HALF_UP);
        String type = request.transactionType().toUpperCase();
        if (!type.equals("CREDIT") && !type.equals("DEBIT")) {
            throw new IllegalArgumentException("transactionType must be CREDIT or DEBIT");
        }

        long startedAt = System.nanoTime();
        Transaction transaction = transactionRepository.save(new Transaction(
                request.transactionId(), account, type, request.amount(), currency, amountInr,
                request.counterparty(), request.channel().toUpperCase(), request.jurisdiction().toUpperCase(),
                request.timestamp()));
        Alert alert = detectionEngine.evaluate(transaction).orElse(null);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("Ingested transaction {} in {} ms; alert={}", transaction.getExternalId(), elapsedMs,
            alert == null ? "none" : alert.getId());
        return new TransactionResult(transaction.getExternalId(), alert == null ? null : alert.getId(), elapsedMs, true);
    }
}