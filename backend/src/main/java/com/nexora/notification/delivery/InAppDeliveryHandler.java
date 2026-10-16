package com.nexora.notification.delivery;

import com.nexora.notification.channel.NotificationChannelFactory;

/** In-app delivery cannot fail transiently (the inbox row exists), so it is not retryable. */
public class InAppDeliveryHandler extends AbstractDeliveryHandler {

    public InAppDeliveryHandler(NotificationChannelFactory family) {
        super(family);
    }

    @Override public boolean retryable() { return false; }
}
