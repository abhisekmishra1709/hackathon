package com.sentinel.aml.detection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sentinel.aml.alert.Alert;
import com.sentinel.aml.alert.AlertRepository;
import com.sentinel.aml.audit.AuditEvent;
import com.sentinel.aml.audit.AuditEventRepository;
import com.sentinel.aml.transaction.ActivitySummary;
import com.sentinel.aml.transaction.Transaction;
import com.sentinel.aml.transaction.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetectionEngine {
        private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    private final AmlRuleProperties properties;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
        private final AuditEventRepository auditRepository;

    public DetectionEngine(AmlRuleProperties properties, TransactionRepository transactionRepository,
                        AlertRepository alertRepository, AuditEventRepository auditRepository) {
        this.properties = properties;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
                this.auditRepository = auditRepository;
    }

    @Transactional
    public Optional<Alert> evaluate(Transaction transaction) {
        int accountWindowHours = Math.max(24,
                Math.max(properties.getStructuringWindowHours(), properties.getRapidMovementHours()));
        List<Transaction> history = transactionRepository
                .findByAccountAndOccurredAtBetweenOrderByOccurredAt(
                        transaction.getAccount(), transaction.getOccurredAt().minus(Duration.ofHours(accountWindowHours)),
                        transaction.getOccurredAt());

        List<RuleMatch> matches = new ArrayList<>();
        threshold(transaction).ifPresent(matches::add);
        structuring(transaction, history).ifPresent(matches::add);
        rapidMovement(transaction, history).ifPresent(matches::add);
        highRisk(transaction).ifPresent(matches::add);
        behavioralDeviation(transaction).ifPresent(matches::add);
        roundNumber(transaction, history).ifPresent(matches::add);

        if (matches.isEmpty()) return Optional.empty();

        String ruleCodes = matches.stream().map(RuleMatch::code).sorted().collect(Collectors.joining(","));
        int riskScore = Math.min(100, matches.stream().mapToInt(RuleMatch::score).max().orElse(0)
                + Math.max(0, matches.size() - 1) * 5);
        String explanation = matches.stream().map(RuleMatch::explanation).collect(Collectors.joining(" "));
        String dedupeKey = transaction.getAccount().getExternalId() + ":" + ruleCodes + ":"
                + transaction.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();

        Optional<Alert> existingAlert = alertRepository.findByDedupeKey(dedupeKey);
        Alert alert = existingAlert
                .orElseGet(() -> new Alert(transaction.getAccount().getCustomer(), transaction.getAccount(),
                        ruleCodes, riskScore, explanation, dedupeKey));
        alert.updateRisk(riskScore, explanation);
        Set<Transaction> evidence = matches.stream().flatMap(match -> match.evidence().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        alert.addEvidence(evidence);
        Alert savedAlert = alertRepository.save(alert);
        auditRepository.save(new AuditEvent("ALERT", savedAlert.getId(),
                existingAlert.isPresent() ? "EVIDENCE_AGGREGATED" : "CREATED", "SYSTEM",
                "Rules=" + ruleCodes + "; riskScore=" + riskScore));
        log.warn("AML alert {} scored {} for account {}; rules={}", savedAlert.getId(), riskScore,
                transaction.getAccount().getExternalId(), ruleCodes);
        return Optional.of(savedAlert);
    }

    Optional<RuleMatch> threshold(Transaction transaction) {
                if (!properties.isThresholdEnabled()) return Optional.empty();
        if (transaction.getAmountInr().compareTo(properties.getReportThresholdInr()) < 0) return Optional.empty();
        return match("CTR_THRESHOLD", 70, "Transaction meets or exceeds the INR reporting threshold.", transaction);
    }

    Optional<RuleMatch> structuring(Transaction transaction, List<Transaction> history) {
                if (!properties.isStructuringEnabled()) return Optional.empty();
        List<Transaction> suspicious = history.stream()
                .filter(item -> !item.getOccurredAt().isBefore(transaction.getOccurredAt()
                        .minus(Duration.ofHours(properties.getStructuringWindowHours()))))
                .filter(item -> item.getAmountInr().compareTo(properties.getStructuringMinimumInr()) >= 0)
                .filter(item -> item.getAmountInr().compareTo(properties.getReportThresholdInr()) < 0)
                .toList();
        if (suspicious.size() < properties.getStructuringCount()) return Optional.empty();
        return Optional.of(new RuleMatch("STRUCTURING", 90,
                suspicious.size() + " just-below-threshold transactions occurred within 24 hours.", suspicious));
    }

    Optional<RuleMatch> rapidMovement(Transaction transaction, List<Transaction> history) {
                if (!properties.isRapidMovementEnabled()) return Optional.empty();
        if (!"DEBIT".equalsIgnoreCase(transaction.getTransactionType())) return Optional.empty();
        for (Transaction deposit : history.stream()
                .filter(item -> "CREDIT".equalsIgnoreCase(item.getTransactionType()))
                .filter(item -> !item.getOccurredAt().isAfter(transaction.getOccurredAt()))
                .filter(item -> !item.getOccurredAt().isBefore(transaction.getOccurredAt()
                        .minus(Duration.ofHours(properties.getRapidMovementHours()))))
                .sorted((left, right) -> right.getOccurredAt().compareTo(left.getOccurredAt()))
                .toList()) {
            List<Transaction> outflows = history.stream()
                    .filter(item -> "DEBIT".equalsIgnoreCase(item.getTransactionType()))
                    .filter(item -> !item.getOccurredAt().isBefore(deposit.getOccurredAt()))
                    .filter(item -> !item.getOccurredAt().isAfter(transaction.getOccurredAt()))
                    .toList();
            BigDecimal totalOutflow = outflows.stream().map(Transaction::getAmountInr)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalOutflow.compareTo(deposit.getAmountInr().multiply(properties.getRapidMovementRatio())) >= 0) {
                List<Transaction> evidence = new ArrayList<>();
                evidence.add(deposit);
                evidence.addAll(outflows);
                return Optional.of(new RuleMatch("RAPID_MOVEMENT", 85,
                        "At least 80% of a recent deposit moved out within 48 hours.", evidence));
            }
        }
        return Optional.empty();
    }

    Optional<RuleMatch> highRisk(Transaction transaction) {
        if (!properties.isHighRiskEnabled()) return Optional.empty();
        boolean highRiskJurisdiction = properties.getHighRiskJurisdictions()
                .contains(transaction.getJurisdiction().toUpperCase());
        boolean highRiskCounterparty = transaction.getCounterparty() != null
                && properties.getHighRiskCounterparties().stream()
                        .anyMatch(name -> name.equalsIgnoreCase(transaction.getCounterparty()));
        if (!highRiskJurisdiction && !highRiskCounterparty) {
            return Optional.empty();
        }
        return match("HIGH_RISK_JURISDICTION", 95,
                "Transaction involves a configured high-risk jurisdiction or counterparty.", transaction);
    }

    Optional<RuleMatch> behavioralDeviation(Transaction transaction, List<Transaction> history) {
                if (!properties.isBehavioralDeviationEnabled()) return Optional.empty();
        LocalDate currentDay = transaction.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();
        List<Transaction> baseline = history.stream()
                .filter(item -> item.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(currentDay))
                .toList();
        if (baseline.isEmpty()) return Optional.empty();
        BigDecimal baselineValue = baseline.stream().map(Transaction::getAmountInr)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Transaction> currentTransactions = history.stream()
                .filter(item -> item.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate().equals(currentDay))
                .toList();
        BigDecimal currentValue = currentTransactions.stream().map(Transaction::getAmountInr)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return behavioralDeviation(transaction, baselineValue, baseline.size(), currentValue,
                currentTransactions.size());
    }

    private Optional<RuleMatch> behavioralDeviation(Transaction transaction) {
                if (!properties.isBehavioralDeviationEnabled()) return Optional.empty();
        LocalDate currentDay = transaction.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();
        Instant currentDayStart = currentDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        ActivitySummary baseline = transactionRepository.summarizeCustomerActivity(
                transaction.getAccount().getCustomer(), currentDayStart.minus(Duration.ofDays(90)), currentDayStart);
        if (baseline.getTransactionCount() == 0) return Optional.empty();
        ActivitySummary current = transactionRepository.summarizeCustomerActivity(
                transaction.getAccount().getCustomer(), currentDayStart, transaction.getOccurredAt().plusNanos(1));
        return behavioralDeviation(transaction, baseline.getTotalAmount(), baseline.getTransactionCount(),
                current.getTotalAmount(), current.getTransactionCount());
    }

    private Optional<RuleMatch> behavioralDeviation(Transaction transaction, BigDecimal baselineValue,
            long baselineCount, BigDecimal currentValue, long currentCount) {
        BigDecimal averageValue = baselineValue.divide(BigDecimal.valueOf(90), 4,
                java.math.RoundingMode.HALF_UP);
        BigDecimal averageVolume = BigDecimal.valueOf(baselineCount).divide(BigDecimal.valueOf(90), 4,
                java.math.RoundingMode.HALF_UP);
        BigDecimal currentVolume = BigDecimal.valueOf(currentCount);
        boolean valueDeviation = currentValue.compareTo(averageValue.multiply(BigDecimal.valueOf(3))) > 0;
        boolean volumeDeviation = currentVolume.compareTo(averageVolume.multiply(BigDecimal.valueOf(3))) > 0;
        if (!valueDeviation && !volumeDeviation) return Optional.empty();
        return match("BEHAVIORAL_DEVIATION", 80,
                "Daily transaction value or volume exceeds three times the 90-day average.", transaction);
    }

    Optional<RuleMatch> roundNumber(Transaction transaction, List<Transaction> history) {
                if (!properties.isRoundNumberEnabled()) return Optional.empty();
        List<Transaction> roundTransactions = history.stream()
                .filter(item -> !item.getOccurredAt().isBefore(transaction.getOccurredAt().minus(Duration.ofHours(24))))
                .filter(item -> item.getAmountInr().remainder(properties.getRoundAmountUnitInr())
                        .compareTo(BigDecimal.ZERO) == 0)
                .toList();
        if (roundTransactions.size() < 2) return Optional.empty();
        return Optional.of(new RuleMatch("ROUND_NUMBER", 45,
                "Repeated round-number transactions occurred within 24 hours.", roundTransactions));
    }

    private Optional<RuleMatch> match(String code, int score, String explanation, Transaction transaction) {
        return Optional.of(new RuleMatch(code, score, explanation, List.of(transaction)));
    }
}