package com.nexora.notification.service;

import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.notification.delivery.DeliveryHandlerCreator;
import com.nexora.notification.dto.NotificationDtos.AttemptDto;
import com.nexora.notification.dto.NotificationDtos.DispatchReport;
import com.nexora.notification.dto.NotificationDtos.NotificationDraft;
import com.nexora.notification.entity.DeliveryAttempt;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.repository.NotificationPreferenceRepository;
import com.nexora.notification.repository.NotificationRepository;
import com.nexora.notification.strategy.ChannelSelectionStrategy;
import com.nexora.notification.strategy.ChannelSelector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Stores the inbox entry and delivers it: Strategy chooses the channels, the Factory Method creator
 * of each channel builds the handler, and the handler uses its Abstract Factory family.
 */
@Service
public class NotificationDispatcher {

    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final UserRepository users;
    private final ChannelSelector selector;
    private final Map<NotificationChannel, DeliveryHandlerCreator> creators = new EnumMap<>(NotificationChannel.class);
    private final Clock clock;

    public NotificationDispatcher(NotificationRepository notifications, NotificationPreferenceRepository preferences,
                                  UserRepository users, ChannelSelector selector, List<DeliveryHandlerCreator> creators,
                                  Clock clock) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.users = users;
        this.selector = selector;
        creators.forEach(c -> this.creators.put(c.channel(), c));
        this.clock = clock;
    }

    @Transactional
    public List<DispatchReport> dispatch(Collection<Long> recipientIds, NotificationDraft draft) {
        List<DispatchReport> reports = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(recipientIds)) {
            if (id == null) {
                continue;
            }
            users.findById(id).filter(User::isActive).ifPresent(u -> reports.add(dispatchOne(u, draft)));
        }
        return reports;
    }

    private DispatchReport dispatchOne(User user, NotificationDraft draft) {
        Notification n = notifications.save(new Notification(user.getId(), draft.category(), draft.severity(),
                truncate(draft.title(), 160), truncate(draft.message(), 1000), draft.link(), draft.sourceModule(),
                clock.instant()));
        Map<NotificationChannel, NotificationPreference> prefs = preferencesOf(user.getId());
        ChannelSelectionStrategy strategy = selector.strategyFor(draft.severity());
        List<NotificationChannel> channels = strategy.select(draft.severity(), prefs);
        List<AttemptDto> attempts = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            DeliveryHandlerCreator creator = creators.get(channel);
            DeliveryAttempt a = creator.deliver(n, user);
            attempts.add(toDto(a, creator.retryable()));
        }
        return new DispatchReport(n.getId(), draft.severity(), strategy.name(), channels, attempts);
    }

    /** Re-runs delivery on the attempt's channel (same Factory Method path). */
    @Transactional
    public AttemptDto retry(DeliveryAttempt previous, Notification notification) {
        User user = users.findById(notification.getRecipientId()).orElseThrow();
        DeliveryHandlerCreator creator = creators.get(previous.getChannel());
        return toDto(creator.deliver(notification, user), creator.retryable());
    }

    public Map<NotificationChannel, NotificationPreference> preferencesOf(Long userId) {
        Map<NotificationChannel, NotificationPreference> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel c : NotificationChannel.values()) {
            map.put(c, NotificationPreference.defaults(userId, c));
        }
        preferences.findByUserId(userId).forEach(p -> map.put(p.getChannel(), p));
        return map;
    }

    public boolean isRetryable(NotificationChannel channel) {
        return creators.get(channel).retryable();
    }

    public Map<NotificationChannel, DeliveryHandlerCreator> creators() {
        return Map.copyOf(creators);
    }

    public static AttemptDto toDto(DeliveryAttempt a, boolean retryableChannel) {
        boolean retryable = retryableChannel && a.getStatus() != DeliveryAttempt.Status.DELIVERED;
        return new AttemptDto(a.getId(), a.getNotificationId(), a.getChannel(), a.getStatus(), a.getAttemptNo(),
                a.getDetail(), a.getCreatedAt(), retryable);
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
