package com.nexora.notification.strategy;

import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.entity.Severity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Routine messages: honour each channel's on/off switch and minimum severity exactly. */
@Component
public class PreferenceBasedChannelStrategy implements ChannelSelectionStrategy {

    @Override public String name() { return "PreferenceBased"; }

    @Override public Set<Severity> severities() { return EnumSet.of(Severity.INFO, Severity.SUCCESS, Severity.WARNING); }

    @Override
    public List<NotificationChannel> select(Severity severity, Map<NotificationChannel, NotificationPreference> prefs) {
        return Arrays.stream(NotificationChannel.values())
                .filter(c -> {
                    NotificationPreference p = prefs.get(c);
                    return p != null && p.isEnabled() && severity.atLeast(p.getMinSeverity());
                })
                .toList();
    }
}
