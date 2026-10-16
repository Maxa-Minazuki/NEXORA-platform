package com.nexora.shared.event;

import java.time.Instant;

public record AppointmentRescheduledEvent(Long appointmentId, String patientName, Long patientUserId,
                                          String doctorName, Long doctorUserId, Instant previousStart,
                                          Instant newStart, Long actorUserId,
                                          Instant occurredAt) implements DomainEvent {
    @Override public String module() { return "CLINIC"; }
    @Override public String summary() { return "Қабылдау ауыстырылды: " + patientName + " - " + doctorName; }
}
