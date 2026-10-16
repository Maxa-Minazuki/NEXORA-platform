package com.nexora.clinic.entity;

import java.util.EnumSet;
import java.util.Set;

/** Appointment lifecycle with an explicit transition table enforced on the backend. */
public enum AppointmentStatus {
    SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW;

    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            case SCHEDULED -> EnumSet.of(CONFIRMED, CANCELLED, COMPLETED, NO_SHOW);
            case CONFIRMED -> EnumSet.of(COMPLETED, CANCELLED, NO_SHOW);
            case COMPLETED, CANCELLED, NO_SHOW -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }

    public boolean isActive() {
        return this == SCHEDULED || this == CONFIRMED;
    }

    public static Set<AppointmentStatus> activeStatuses() {
        return EnumSet.of(SCHEDULED, CONFIRMED);
    }
}
