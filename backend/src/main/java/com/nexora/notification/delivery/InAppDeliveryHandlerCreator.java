package com.nexora.notification.delivery;

import com.nexora.notification.channel.inapp.InAppChannelFactory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.repository.DeliveryAttemptRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Concrete creator for the in-app channel. */
@Component
public class InAppDeliveryHandlerCreator extends DeliveryHandlerCreator {

    private final InAppChannelFactory family;

    public InAppDeliveryHandlerCreator(DeliveryAttemptRepository attempts, Clock clock, InAppChannelFactory family) {
        super(attempts, clock);
        this.family = family;
    }

    @Override public NotificationChannel channel() { return NotificationChannel.IN_APP; }

    @Override protected DeliveryHandler createHandler() { return new InAppDeliveryHandler(family); }
}
