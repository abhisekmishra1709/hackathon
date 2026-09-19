package com.sentinel.aml.alert;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.customer.Customer;
import com.sentinel.aml.transaction.Transaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "disposition_reason")
    private String dispositionReason;

    @Column(name = "dedupe_key", unique = true)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany
    @JoinTable(name = "alert_transactions",
            joinColumns = @JoinColumn(name = "alert_id"),
            inverseJoinColumns = @JoinColumn(name = "transaction_id"))
    private Set<Transaction> evidence = new LinkedHashSet<>();

    protected Alert() {
    }

    public Alert(Customer customer, Account account, String ruleCode, int riskScore, String explanation,
            String dedupeKey) {
        this.customer = customer;
        this.account = account;
        this.ruleCode = ruleCode;
        this.riskScore = riskScore;
        this.explanation = explanation;
        this.dedupeKey = dedupeKey;
    }

    public void addEvidence(Iterable<Transaction> transactions) {
        transactions.forEach(evidence::add);
        updatedAt = Instant.now();
    }

    public void updateRisk(int score, String newExplanation) {
        riskScore = Math.max(riskScore, score);
        explanation = newExplanation;
        updatedAt = Instant.now();
    }

    public void assign(String analyst) {
        assignedTo = analyst;
        status = "INVESTIGATING";
        updatedAt = Instant.now();
    }

    public void close(String analyst, String reason) {
        assignedTo = analyst;
        dispositionReason = reason;
        status = "CLOSED";
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Account getAccount() { return account; }
    public String getRuleCode() { return ruleCode; }
    public int getRiskScore() { return riskScore; }
    public String getExplanation() { return explanation; }
    public String getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public String getDispositionReason() { return dispositionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<Transaction> getEvidence() { return Set.copyOf(evidence); }
}