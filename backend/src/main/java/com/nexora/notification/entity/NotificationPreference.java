package com.nexora.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", nullable = false, length = 10)
    private Severity minSeverity;

    protected NotificationPreference() {
    }

    public NotificationPreference(Long userId, NotificationChannel channel, boolean enabled, Severity minSeverity) {
        this.userId = userId;
        this.channel = channel;
        this.enabled = enabled;
        this.minSeverity = minSeverity;
    }

    public static NotificationPreference defaults(Long userId, NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP
                ? new NotificationPreference(userId, channel, true, Severity.INFO)
                : new NotificationPreference(userId, channel, true, Severity.WARNING);
    }

    public void update(boolean enabled, Severity minSeverity) {
        this.enabled = enabled;
        this.minSeverity = minSeverity;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public boolean isEnabled() { return enabled; }
    public Severity getMinSeverity() { return minSeverity; }
}
