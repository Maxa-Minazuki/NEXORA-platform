package com.nexora.notification.delivery;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.SendResult;
import com.nexora.notification.entity.DeliveryAttempt;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.repository.DeliveryAttemptRepository;

import java.time.Clock;

/**
 * <h2>Design pattern: Factory Method (creator)</h2>
 *
 * {@link #deliver} is the fixed algorithm every channel shares: create a handler, run it, persist the
 * {@link DeliveryAttempt}. Which handler is created is deferred to subclasses through the factory
 * method {@link #createHandler()}. Adding SMS means one new creator + one new handler.
 *
 * <p>Difference to the Abstract Factory used inside the handler: this creator produces <em>one</em>
 * product (the handler with its retry semantics); {@code NotificationChannelFactory} produces a
 * <em>family</em> of matching components.</p>
 */
public abstract class DeliveryHandlerCreator {

    private final DeliveryAttemptRepository attempts;
    private final Clock clock;

    protected DeliveryHandlerCreator(DeliveryAttemptRepository attempts, Clock clock) {
        this.attempts = attempts;
        this.clock = clock;
    }

    public abstract NotificationChannel channel();

    /** The factory method. */
    protected abstract DeliveryHandler createHandler();

    public final DeliveryAttempt deliver(Notification notification, User recipient) {
        DeliveryHandler handler = createHandler();
        SendResult result;
        try {
            result = handler.handle(notification, recipient);
        } catch (RuntimeException ex) {
            result = SendResult.failed("Жеткізудегі күтпеген қате: " + ex.getMessage());
        }
        int attemptNo = attempts.lastAttemptNo(notification.getId(), channel()) + 1;
        return attempts.save(new DeliveryAttempt(notification.getId(), channel(), result.status(), attemptNo,
                result.detail(), clock.instant()));
    }

    public final boolean retryable() {
        return createHandler().retryable();
    }
}
