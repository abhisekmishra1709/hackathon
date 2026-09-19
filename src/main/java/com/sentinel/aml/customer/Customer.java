package com.sentinel.aml.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String city;
    private String state;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "annual_income")
    private BigDecimal annualIncome;

    @Column(name = "customer_segment")
    private String customerSegment;

    @Column(name = "kyc_status")
    private String kycStatus;

    @Column(name = "risk_rating", nullable = false)
    private String riskRating;

    @Column(name = "politically_exposed", nullable = false)
    private boolean politicallyExposed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Customer() {
    }

    public Customer(String externalId) {
        this.externalId = externalId;
    }

    public void update(String firstName, String lastName, LocalDate dateOfBirth, String email, String phoneNumber,
            String city, String state, String countryCode, BigDecimal annualIncome, String customerSegment,
            String kycStatus, String riskRating, boolean politicallyExposed) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = (firstName + " " + lastName).trim();
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
        this.annualIncome = annualIncome;
        this.customerSegment = customerSegment;
        this.kycStatus = kycStatus;
        this.riskRating = riskRating;
        this.politicallyExposed = politicallyExposed;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountryCode() { return countryCode; }
    public String getRiskRating() { return riskRating; }
    public boolean isPoliticallyExposed() { return politicallyExposed; }
}