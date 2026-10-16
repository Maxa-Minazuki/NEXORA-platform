package com.nexora.notification.channel.inapp;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.FormattedMessage;
import com.nexora.notification.channel.MessageFormatter;
import com.nexora.notification.entity.Notification;

/** Short toast-friendly text. */
public class InAppMessageFormatter implements MessageFormatter {
    @Override
    public FormattedMessage format(Notification n, User recipient) {
        String body = n.getMessage().length() > 180 ? n.getMessage().substring(0, 177) + "..." : n.getMessage();
        return new FormattedMessage(n.getTitle(), body);
    }
}
