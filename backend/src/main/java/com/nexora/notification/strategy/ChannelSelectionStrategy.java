package com.nexora.notification.strategy;

import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.entity.Severity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h2>Design pattern: Strategy - which channels deliver this notification?</h2>
 *
 * The dispatcher picks the strategy registered for the notification's severity (a map lookup, no
 * if/else chain) and lets it decide the channels based on the user's preferences.
 */
public interface ChannelSelectionStrategy {

    String name();

    Set<Severity> severities();

    List<NotificationChannel> select(Severity severity, Map<NotificationChannel, NotificationPreference> preferences);
}
