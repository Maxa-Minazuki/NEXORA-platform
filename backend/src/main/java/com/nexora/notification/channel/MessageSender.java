package com.nexora.notification.channel;

import com.nexora.auth.entity.User;
import com.nexora.notification.entity.Notification;

/** Abstract product #2 of the channel family. */
public interface MessageSender {
    SendResult send(FormattedMessage message, Notification notification, User recipient);
}
