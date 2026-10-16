package com.nexora.notification.channel;

import com.nexora.auth.entity.User;

/** Abstract product #3 of the channel family. */
public interface ChannelAvailabilityChecker {
    Availability check(User recipient);

    record Availability(boolean available, String reason) {
        public static Availability yes() { return new Availability(true, "қолжетімді"); }
        public static Availability no(String reason) { return new Availability(false, reason); }
    }
}
