package com.nexora.notification.channel.inapp;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.ChannelAvailabilityChecker;

public class InAppAvailabilityChecker implements ChannelAvailabilityChecker {
    @Override
    public Availability check(User recipient) {
        return recipient.isActive() ? Availability.yes() : Availability.no("Тіркелгі бұғатталған");
    }
}
