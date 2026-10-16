package com.nexora.notification.delivery;

import com.nexora.notification.channel.email.EmailChannelFactory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.repository.DeliveryAttemptRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Concrete creator for the e-mail channel. */
@Component
public class EmailDeliveryHandlerCreator extends DeliveryHandlerCreator {

    private final EmailChannelFactory family;

    public EmailDeliveryHandlerCreator(DeliveryAttemptRepository attempts, Clock clock, EmailChannelFactory family) {
        super(attempts, clock);
        this.family = family;
    }

    @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }

    @Override protected DeliveryHandler createHandler() { return new EmailDeliveryHandler(family); }
}
