package com.nexora.notification.channel.email;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.FormattedMessage;
import com.nexora.notification.channel.MessageFormatter;
import com.nexora.notification.entity.Notification;

/** Full plain-text e-mail with greeting, severity tag and footer. */
public class EmailMessageFormatter implements MessageFormatter {

    private final String platformName;

    EmailMessageFormatter(String platformName) {
        this.platformName = platformName;
    }

    @Override
    public FormattedMessage format(Notification n, User recipient) {
        String subject = "[" + platformName + "][" + n.getSeverity() + "] " + n.getTitle();
        String body = "Құрметті " + recipient.getFullName() + ",\n\n" + n.getMessage()
                + (n.getLink() == null ? "" : "\n\n" + platformName + " жүйесінде ашу: " + n.getLink())
                + "\n\n-- \nБұл хатты " + platformName + " оқу-демонстрациялық платформасы жасады.";
        return new FormattedMessage(subject, body);
    }
}
