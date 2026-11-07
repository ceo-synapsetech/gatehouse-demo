package com.gatehouse.demo;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AppNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String recipient;
    private Long requestId;
    private String title;
    private String message;
    private Instant createdAt;
    private Instant readAt;

    protected AppNotification() {}
    public AppNotification(String recipient, Long requestId, String title, String message) {
        this.recipient = recipient;
        this.requestId = requestId;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public Long getRequestId() { return requestId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}
