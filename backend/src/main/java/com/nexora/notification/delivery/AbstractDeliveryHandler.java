package com.nexora.notification.delivery;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.ChannelAvailabilityChecker.Availability;
import com.nexora.notification.channel.NotificationChannelFactory;
import com.nexora.notification.channel.SendResult;
import com.nexora.notification.entity.Notification;

/**
 * Client of the Abstract Factory: it asks its factory for the channel family and uses it through the
 * abstract product interfaces only (check availability -> format -> send).
 */
public abstract class AbstractDeliveryHandler implements DeliveryHandler {

    private final NotificationChannelFactory family;

    protected AbstractDeliveryHandler(NotificationChannelFactory family) {
        this.family = family;
    }

    @Override
    public SendResult handle(Notification notification, User recipient) {
        Availability availability = family.createAvailabilityChecker().check(recipient);
        if (!availability.available()) {
            return SendResult.skipped(availability.reason());
        }
        var message = family.createFormatter().format(notification, recipient);
        return family.createSender().send(message, notification, recipient);
    }
}
