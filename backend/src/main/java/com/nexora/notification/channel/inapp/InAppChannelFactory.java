package com.nexora.notification.channel.inapp;

import com.nexora.notification.channel.ChannelAvailabilityChecker;
import com.nexora.notification.channel.MessageFormatter;
import com.nexora.notification.channel.MessageSender;
import com.nexora.notification.channel.NotificationChannelFactory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.realtime.SseHub;
import org.springframework.stereotype.Component;

/** Concrete factory: the in-app family (inbox + live push over Server-Sent Events). */
@Component
public class InAppChannelFactory implements NotificationChannelFactory {

    private final SseHub hub;

    public InAppChannelFactory(SseHub hub) {
        this.hub = hub;
    }

    @Override public NotificationChannel channel() { return NotificationChannel.IN_APP; }
    @Override public MessageFormatter createFormatter() { return new InAppMessageFormatter(); }
    @Override public MessageSender createSender() { return new InAppMessageSender(hub); }
    @Override public ChannelAvailabilityChecker createAvailabilityChecker() { return new InAppAvailabilityChecker(); }
}
