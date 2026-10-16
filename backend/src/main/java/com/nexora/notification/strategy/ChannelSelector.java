package com.nexora.notification.strategy;

import com.nexora.notification.entity.Severity;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelSelector {

    private final Map<Severity, ChannelSelectionStrategy> bySeverity = new EnumMap<>(Severity.class);

    public ChannelSelector(List<ChannelSelectionStrategy> strategies) {
        for (ChannelSelectionStrategy s : strategies) {
            for (Severity sev : s.severities()) {
                if (bySeverity.putIfAbsent(sev, s) != null) {
                    throw new IllegalStateException("Two channel strategies for severity " + sev);
                }
            }
        }
        for (Severity sev : Severity.values()) {
            if (!bySeverity.containsKey(sev)) {
                throw new IllegalStateException("No channel strategy for severity " + sev);
            }
        }
    }

    public ChannelSelectionStrategy strategyFor(Severity severity) {
        return bySeverity.get(severity);
    }
}
