package com.sentinel.aml.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import com.sentinel.aml.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "amount_inr", nullable = false)
    private BigDecimal amountInr;

    private String counterparty;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String jurisdiction;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() {
    }

    public Transaction(String externalId, Account account, String transactionType, BigDecimal amount,
            String currency, BigDecimal amountInr, String counterparty, String channel, String jurisdiction,
            Instant occurredAt) {
        this.externalId = externalId;
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.amountInr = amountInr;
        this.counterparty = counterparty;
        this.channel = channel;
        this.jurisdiction = jurisdiction;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public Account getAccount() { return account; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmountInr() { return amountInr; }
    public String getCounterparty() { return counterparty; }
    public String getJurisdiction() { return jurisdiction; }
    public Instant getOccurredAt() { return occurredAt; }
}