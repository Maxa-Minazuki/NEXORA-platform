package com.nexora.clinic.entity;

import com.nexora.shared.exception.InvalidStateTransitionException;
import com.nexora.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "appointments")
public class Appointment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 1000)
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Version
    private long version;

    protected Appointment() {
    }

    public Appointment(Patient patient, Doctor doctor, Instant startTime, Instant endTime, String reason, Long createdBy) {
        this.patient = patient;
        this.doctor = doctor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    public void transitionTo(AppointmentStatus next) {
        if (!status.allowedNext().contains(next)) {
            throw new InvalidStateTransitionException("appointment", status.name(), "move to " + next.name());
        }
        this.status = next;
    }

    public void cancel(String reason) {
        transitionTo(AppointmentStatus.CANCELLED);
        this.cancelReason = reason;
    }

    public void reschedule(Instant start, Instant end) {
        if (!status.isActive()) {
            throw new InvalidStateTransitionException("appointment", status.name(), "reschedule");
        }
        this.startTime = start;
        this.endTime = end;
        this.status = AppointmentStatus.SCHEDULED;
    }

    public void setNotes(String notes) { this.notes = notes; }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public AppointmentStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public String getCancelReason() { return cancelReason; }
    public Long getCreatedBy() { return createdBy; }
}
