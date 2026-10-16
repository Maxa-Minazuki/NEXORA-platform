package com.nexora.notification.channel;

import com.nexora.notification.entity.NotificationChannel;

/**
 * <h2>Design pattern: Abstract Factory</h2>
 *
 * Creates a <em>consistent family</em> of components for one delivery channel: a formatter, a sender
 * and an availability checker that belong together (an e-mail formatter must never be combined with
 * the in-app sender). Client code ({@code AbstractDeliveryHandler}) only knows these interfaces; the
 * concrete family is selected by choosing the factory.
 */
public interface NotificationChannelFactory {

    NotificationChannel channel();

    MessageFormatter createFormatter();

    MessageSender createSender();

    ChannelAvailabilityChecker createAvailabilityChecker();
}
