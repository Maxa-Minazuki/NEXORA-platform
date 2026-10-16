package com.nexora.shared.event;

import java.time.Instant;

public record AppointmentCancelledEvent(Long appointmentId, String patientName, Long patientUserId,
                                        String doctorName, Long doctorUserId, Instant startTime, String reason,
                                        Long actorUserId, Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "CLINIC"; }
    @Override public String summary() { return "Қабылдау болдырылмады: " + patientName + " - " + doctorName; }
}
