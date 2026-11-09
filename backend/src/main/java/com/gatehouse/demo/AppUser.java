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

    protected AppUser() {}

    public AppUser(String username, String passwordHash, String role, String displayName, String unit) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public String getUnit() { return unit; }
    public boolean isEnabled() { return enabled; }
}
