package com.sentinel.aml.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.sentinel.aml.customer.Customer;
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
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "account_status", nullable = false)
    private String accountStatus;

    @Column(nullable = false)
    private String currency;

    @Column(name = "risk_rating", nullable = false)
    private String riskRating;

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "account_tier")
    private String accountTier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(String externalId) {
        this.externalId = externalId;
    }

    public void update(Customer customer, String accountType, String accountStatus, String currency,
            String riskRating, LocalDate openedOn, BigDecimal currentBalance, String branchCode, String accountTier) {
        this.customer = customer;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.currency = currency;
        this.riskRating = riskRating;
        this.openedOn = openedOn;
        this.currentBalance = currentBalance;
        this.branchCode = branchCode;
        this.accountTier = accountTier;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public Customer getCustomer() { return customer; }
    public String getCurrency() { return currency; }
}