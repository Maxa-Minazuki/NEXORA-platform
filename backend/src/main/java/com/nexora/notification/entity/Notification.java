package com.nexora.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Inbox entry of one recipient. The inbox is the system of record; channels are delivery attempts. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 255)
    private String link;

    @Column(name = "source_module", nullable = false, length = 20)
    private String sourceModule;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Long recipientId, NotificationCategory category, Severity severity, String title, String message,
                        String link, String sourceModule, Instant createdAt) {
        this.recipientId = recipientId;
        this.category = category;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.link = link;
        this.sourceModule = sourceModule;
        this.createdAt = createdAt;
    }

    public void markRead(Instant at) {
        if (readAt == null) {
            readAt = at;
        }
    }

    public Long getId() { return id; }
    public Long getRecipientId() { return recipientId; }
    public NotificationCategory getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public String getSourceModule() { return sourceModule; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
