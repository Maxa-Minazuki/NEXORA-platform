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

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    public enum Status { DELIVERED, FAILED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeliveryAttempt() {
    }

    public DeliveryAttempt(Long notificationId, NotificationChannel channel, Status status, int attemptNo, String detail,
                           Instant createdAt) {
        this.notificationId = notificationId;
        this.channel = channel;
        this.status = status;
        this.attemptNo = attemptNo;
        this.detail = detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getNotificationId() { return notificationId; }
    public NotificationChannel getChannel() { return channel; }
    public Status getStatus() { return status; }
    public int getAttemptNo() { return attemptNo; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
