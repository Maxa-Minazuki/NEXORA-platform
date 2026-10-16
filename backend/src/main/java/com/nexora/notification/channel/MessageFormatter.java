package com.nexora.notification.channel;

import com.nexora.auth.entity.User;
import com.nexora.notification.entity.Notification;

/** Abstract product #1 of the channel family. */
public interface MessageFormatter {
    FormattedMessage format(Notification notification, User recipient);
}
