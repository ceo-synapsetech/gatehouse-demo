package com.gatehouse.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @Column(length = 120, nullable = false, updatable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "account_role", length = 20, nullable = false)
    private String role;

    @Column(length = 120, nullable = false)
    private String displayName;

    @Column(length = 32)
    private String unit;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "ACTIVE";

    @Column(name = "approved_by_603")
    private Boolean approvedBy603 = false;

    @Column(name = "approved_by_second")
    private Boolean approvedBySecond = false;

    protected AppUser() {}

    public AppUser(String username, String passwordHash, String role, String displayName, String unit) {
        this(username, passwordHash, role, displayName, unit, "ACTIVE");
    }

    public AppUser(String username, String passwordHash, String role, String displayName, String unit, String approvalStatus) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.unit = unit;
        this.approvalStatus = approvalStatus;
        this.enabled = "ACTIVE".equals(approvalStatus);
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public String getUnit() { return unit; }
    public String getApprovalStatus() { return approvalStatus == null ? "ACTIVE" : approvalStatus; }
    public boolean isEnabled() { return enabled && "ACTIVE".equals(getApprovalStatus()); }
    public boolean isApprovedBy603() { return Boolean.TRUE.equals(approvedBy603); }
    public boolean isApprovedBySecond() { return Boolean.TRUE.equals(approvedBySecond); }

    public void approveBy(String unit, String secondApproverUnit) {
        if ("A-603".equals(unit)) approvedBy603 = true;
        if (secondApproverUnit.equals(unit)) approvedBySecond = true;
        if (isApprovedBy603() && isApprovedBySecond()) {
            approvalStatus = "ACTIVE";
            enabled = true;
        }
    }

    public void reject() {
        approvalStatus = "REJECTED";
        enabled = false;
    }
}
