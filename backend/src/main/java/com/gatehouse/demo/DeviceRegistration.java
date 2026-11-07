package com.gatehouse.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DeviceRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String recipient;
    @Column(length = 4096, unique = true) private String token;

    protected DeviceRegistration() {}
    public DeviceRegistration(String recipient, String token) {
        this.recipient = recipient;
        this.token = token;
    }
    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getToken() { return token; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
}
