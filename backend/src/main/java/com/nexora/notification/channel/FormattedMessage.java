package com.nexora.notification.channel;

/** Channel-specific rendering of a notification. */
public record FormattedMessage(String subject, String body) {
}
