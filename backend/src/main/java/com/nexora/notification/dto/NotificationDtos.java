package com.nexora.notification.dto;

import com.nexora.notification.entity.DeliveryAttempt;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.Severity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationDto(Long id, NotificationCategory category, Severity severity, String title,
                                  String message, String link, String sourceModule, boolean read, Instant readAt,
                                  Instant createdAt, List<AttemptDto> deliveries) {
    }

    public record AttemptDto(Long id, Long notificationId, NotificationChannel channel, DeliveryAttempt.Status status,
                             int attemptNo, String detail, Instant createdAt, boolean retryable) {
    }

    public record PreferenceDto(@NotNull NotificationChannel channel, boolean enabled, @NotNull Severity minSeverity) {
    }

    public record PreferencesRequest(@NotEmpty List<@Valid PreferenceDto> preferences) {
    }

    public record ChannelInfo(NotificationChannel channel, String factory, List<String> family, boolean available,
                              String availabilityReason, String creator, boolean retryable) {
    }

    public record TestNotificationRequest(@NotNull Severity severity,
                                          @NotBlank @Size(max = 160) String title,
                                          @NotBlank @Size(max = 1000) String message) {
    }

    public record DispatchReport(Long notificationId, Severity severity, String strategy,
                                 List<NotificationChannel> selectedChannels, List<AttemptDto> attempts) {
    }

    /** What business modules hand to the dispatcher. */
    public record NotificationDraft(NotificationCategory category, Severity severity, String title, String message,
                                    String link, String sourceModule) {
    }
}
