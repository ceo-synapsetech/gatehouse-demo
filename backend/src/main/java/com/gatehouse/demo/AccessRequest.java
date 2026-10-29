package com.gatehouse.demo;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AccessRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String unit;
    private String kind;
    private String person;
    private String details;
    private Instant createdAt;
    @Enumerated(EnumType.STRING) private Status status;

    protected AccessRequest() {}
    public AccessRequest(String unit, String kind, String person, String details) {
        this.unit = unit; this.kind = kind; this.person = person; this.details = details;
        this.createdAt = Instant.now(); this.status = Status.PENDING;
    }
    public Long getId() { return id; }
    public String getUnit() { return unit; }
    public String getKind() { return kind; }
    public String getPerson() { return person; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public enum Status { PENDING, APPROVED, DENIED }
}
