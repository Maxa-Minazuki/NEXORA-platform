package com.nexora.notification.channel.inapp;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.FormattedMessage;
import com.nexora.notification.channel.MessageSender;
import com.nexora.notification.channel.SendResult;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.realtime.SseHub;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * The notification row itself is the in-app inbox entry. This sender additionally pushes a live
 * event to the recipient's open browser sessions - after commit, so the client never fetches an
 * inbox that does not contain the notification yet.
 */
public class InAppMessageSender implements MessageSender {

    private final SseHub hub;

    InAppMessageSender(SseHub hub) {
        this.hub = hub;
    }

    @Override
    public SendResult send(FormattedMessage message, Notification n, User recipient) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", n.getId());
        payload.put("title", message.subject());
        payload.put("message", message.body());
        payload.put("severity", n.getSeverity().name());
        payload.put("category", n.getCategory().name());
        payload.put("link", n.getLink() == null ? "" : n.getLink());
        int sessions = hub.sessionCount(recipient.getId());
        Runnable push = () -> hub.sendToUser(recipient.getId(), "notification", payload);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
        return SendResult.delivered("Кіріс жәшігіне сақталды; ашық сеанстарға жіберілді: " + sessions);
    }
}
