package com.nexora.notification.strategy;

import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.entity.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Critical messages: always pushed in-app (even if muted) and sent on every other channel the user
 * has not switched off, ignoring the minimum-severity filter.
 */
@Component
public class CriticalEscalationChannelStrategy implements ChannelSelectionStrategy {

    @Override public String name() { return "CriticalEscalation"; }

    @Override public Set<Severity> severities() { return EnumSet.of(Severity.CRITICAL); }

    @Override
    public List<NotificationChannel> select(Severity severity, Map<NotificationChannel, NotificationPreference> prefs) {
        List<NotificationChannel> channels = new ArrayList<>();
        channels.add(NotificationChannel.IN_APP);
        for (NotificationChannel c : NotificationChannel.values()) {
            NotificationPreference p = prefs.get(c);
            if (c != NotificationChannel.IN_APP && p != null && p.isEnabled()) {
                channels.add(c);
            }
        }
        return channels;
    }
}
