package com.nexora.notification.delivery;

import com.nexora.notification.channel.NotificationChannelFactory;

/** E-mail can fail transiently (SMTP down, not yet configured), so a manual retry is allowed. */
public class EmailDeliveryHandler extends AbstractDeliveryHandler {

    public EmailDeliveryHandler(NotificationChannelFactory family) {
        super(family);
    }

    @Override public boolean retryable() { return true; }
}
