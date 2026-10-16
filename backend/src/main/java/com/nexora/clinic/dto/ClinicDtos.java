package com.nexora.clinic.dto;

import com.nexora.clinic.entity.AppointmentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ClinicDtos {

    private ClinicDtos() {
    }

    public record SpecialtyDto(Long id, String name, String description) {
    }

    public record ScheduleDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }

    public record DoctorDto(Long id, String fullName, Long specialtyId, String specialty, String email, String phone,
                            String room, String bio, List<ScheduleDto> schedule) {
    }

    public record PatientDto(Long id, String firstName, String lastName, String fullName, LocalDate dateOfBirth,
                             String gender, String phone, String email, String notes, boolean hasAccount,
                             Instant createdAt) {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Pattern(regexp = "FEMALE|MALE|OTHER") String gender,
            @Size(max = 40) @Pattern(regexp = "^[+0-9 ()-]*$", message = "тек цифрлар, бос орын және + ( ) - белгілері") String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 1000) String notes) {
    }

    public record AppointmentDto(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                 String specialty, String room, Instant startTime, Instant endTime,
                                 AppointmentStatus status, String reason, String notes, String cancelReason,
                                 List<AppointmentStatus> allowedTransitions, Instant createdAt) {
    }

    public record BookAppointmentRequest(@NotNull Long patientId, @NotNull Long doctorId,
                                         @NotNull @Future Instant startTime,
                                         @NotBlank @Size(max = 255) String reason) {
    }

    public record RescheduleRequest(@NotNull @Future Instant startTime) {
    }

    public record CancelRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record StatusChangeRequest(@NotNull AppointmentStatus status, @Size(max = 1000) String notes) {
    }

    public record SlotDto(Instant start, Instant end, boolean available) {
    }

    public record DaySlots(Long doctorId, LocalDate date, String timezone, int slotMinutes, boolean workingDay,
                           List<SlotDto> slots) {
    }
}
