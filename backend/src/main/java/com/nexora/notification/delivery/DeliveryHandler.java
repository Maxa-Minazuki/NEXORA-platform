package com.nexora.notification.delivery;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.SendResult;
import com.nexora.notification.entity.Notification;

/** Product of the Factory Method: performs one delivery over one channel. */
public interface DeliveryHandler {

    SendResult handle(Notification notification, User recipient);

    /** Whether a failed or skipped attempt may be retried safely. */
    boolean retryable();
}
