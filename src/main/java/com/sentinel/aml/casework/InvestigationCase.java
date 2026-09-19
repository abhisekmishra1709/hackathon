package com.sentinel.aml.casework;

import java.time.Instant;

import com.sentinel.aml.alert.Alert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cases")
public class InvestigationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", unique = true)
    private Alert alert;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "analyst_id", nullable = false)
    private String analystId;

    private String notes;
    private String disposition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected InvestigationCase() {
    }

    public InvestigationCase(Alert alert, String analystId, String notes) {
        this.alert = alert;
        this.analystId = analystId;
        this.notes = notes;
    }

    public void update(String status, String notes, String disposition) {
        this.status = status;
        this.notes = notes;
        this.disposition = disposition;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Alert getAlert() { return alert; }
    public String getStatus() { return status; }
    public String getAnalystId() { return analystId; }
    public String getNotes() { return notes; }
    public String getDisposition() { return disposition; }
    public Instant getCreatedAt() { return createdAt; }
}