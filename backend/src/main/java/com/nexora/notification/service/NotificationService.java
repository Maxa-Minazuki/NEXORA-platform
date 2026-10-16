package com.nexora.notification.service;

import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.notification.channel.NotificationChannelFactory;
import com.nexora.notification.dto.NotificationDtos.AttemptDto;
import com.nexora.notification.dto.NotificationDtos.ChannelInfo;
import com.nexora.notification.dto.NotificationDtos.NotificationDto;
import com.nexora.notification.dto.NotificationDtos.PreferenceDto;
import com.nexora.notification.entity.DeliveryAttempt;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.entity.Severity;
import com.nexora.notification.repository.DeliveryAttemptRepository;
import com.nexora.notification.repository.NotificationPreferenceRepository;
import com.nexora.notification.repository.NotificationRepository;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final DeliveryAttemptRepository attempts;
    private final NotificationDispatcher dispatcher;
    private final List<NotificationChannelFactory> factories;
    private final UserRepository users;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, NotificationPreferenceRepository preferences,
                               DeliveryAttemptRepository attempts, NotificationDispatcher dispatcher,
                               List<NotificationChannelFactory> factories, UserRepository users, Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.attempts = attempts;
        this.dispatcher = dispatcher;
        this.factories = factories;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, boolean unreadOnly, Severity severity,
                                              NotificationCategory category, int page, int size) {
        var pageable = PageRequests.of(page, size, null, Set.of(), "-createdAt");
        return PageResponse.of(notifications.search(userId, unreadOnly, severity, category, pageable), n -> toDto(n, true));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationDto markRead(Long userId, Long id) {
        Notification n = owned(userId, id);
        n.markRead(clock.instant());
        return toDto(n, true);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllRead(userId, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<PreferenceDto> preferences(Long userId) {
        return dispatcher.preferencesOf(userId).values().stream()
                .map(p -> new PreferenceDto(p.getChannel(), p.isEnabled(), p.getMinSeverity())).toList();
    }

    @Transactional
    public List<PreferenceDto> updatePreferences(Long userId, List<PreferenceDto> requested) {
        Map<NotificationChannel, NotificationPreference> existing = new java.util.EnumMap<>(NotificationChannel.class);
        preferences.findByUserId(userId).forEach(p -> existing.put(p.getChannel(), p));
        for (PreferenceDto dto : requested) {
            NotificationPreference p = existing.get(dto.channel());
            if (p == null) {
                preferences.save(new NotificationPreference(userId, dto.channel(), dto.enabled(), dto.minSeverity()));
            } else {
                p.update(dto.enabled(), dto.minSeverity());
            }
        }
        return preferences(userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptDto> deliveries(AuthenticatedUser user, int page, int size) {
        Long filter = user.hasRole(Role.ADMIN) ? null : user.id();
        var pageable = PageRequests.of(page, size, null, Set.of(), "-createdAt");
        return PageResponse.of(attempts.search(filter, pageable),
                a -> NotificationDispatcher.toDto(a, dispatcher.isRetryable(a.getChannel())));
    }

    @Transactional
    public AttemptDto retry(AuthenticatedUser user, Long attemptId) {
        DeliveryAttempt attempt = attempts.findById(attemptId).orElseThrow(() -> new NotFoundException("Delivery attempt", attemptId));
        Notification n = notifications.findById(attempt.getNotificationId()).orElseThrow();
        if (!user.hasRole(Role.ADMIN) && !n.getRecipientId().equals(user.id())) {
            throw new NotFoundException("Delivery attempt", attemptId);
        }
        if (attempt.getStatus() == DeliveryAttempt.Status.DELIVERED || !dispatcher.isRetryable(attempt.getChannel())) {
            throw new BusinessRuleException("NOT_RETRYABLE", "Тек сәтсіз немесе жіберілмеген e-mail хаттарын қайталауға болады");
        }
        return dispatcher.retry(attempt, n);
    }

    @Transactional(readOnly = true)
    public List<ChannelInfo> channels(Long userId) {
        User user = users.findById(userId).orElseThrow();
        return factories.stream().map(f -> {
            var availability = f.createAvailabilityChecker().check(user);
            var creator = dispatcher.creators().get(f.channel());
            return new ChannelInfo(f.channel(), f.getClass().getSimpleName(), List.of(
                    f.createFormatter().getClass().getSimpleName(), f.createSender().getClass().getSimpleName(),
                    f.createAvailabilityChecker().getClass().getSimpleName()),
                    availability.available(), availability.reason(), creator.getClass().getSimpleName(), creator.retryable());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> latest(Long userId) {
        return notifications.findTop5ByRecipientIdOrderByCreatedAtDesc(userId).stream().map(n -> toDto(n, false)).toList();
    }

    private Notification owned(Long userId, Long id) {
        Notification n = notifications.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
        if (!n.getRecipientId().equals(userId)) {
            throw new NotFoundException("Notification", id);
        }
        return n;
    }

    private NotificationDto toDto(Notification n, boolean withDeliveries) {
        List<AttemptDto> deliveries = withDeliveries
                ? attempts.findByNotificationIdOrderByIdAsc(n.getId()).stream()
                .map(a -> NotificationDispatcher.toDto(a, dispatcher.isRetryable(a.getChannel()))).toList()
                : List.of();
        return new NotificationDto(n.getId(), n.getCategory(), n.getSeverity(), n.getTitle(), n.getMessage(), n.getLink(),
                n.getSourceModule(), n.getReadAt() != null, n.getReadAt(), n.getCreatedAt(), deliveries);
    }
}
