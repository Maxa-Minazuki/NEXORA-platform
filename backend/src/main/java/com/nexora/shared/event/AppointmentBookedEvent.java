package com.nexora.shared.event;

import java.time.Instant;

public record AppointmentBookedEvent(Long appointmentId, String patientName, Long patientUserId, String doctorName,
                                     Long doctorUserId, Instant startTime, Long actorUserId,
                                     Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "CLINIC"; }
    @Override public String summary() { return "Қабылдауға жазылу: " + patientName + " - " + doctorName; }
}
