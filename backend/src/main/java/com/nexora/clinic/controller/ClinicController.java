package com.nexora.clinic.controller;

import com.nexora.clinic.dto.ClinicDtos.AppointmentDto;
import com.nexora.clinic.dto.ClinicDtos.BookAppointmentRequest;
import com.nexora.clinic.dto.ClinicDtos.CancelRequest;
import com.nexora.clinic.dto.ClinicDtos.DaySlots;
import com.nexora.clinic.dto.ClinicDtos.DoctorDto;
import com.nexora.clinic.dto.ClinicDtos.PatientDto;
import com.nexora.clinic.dto.ClinicDtos.PatientRequest;
import com.nexora.clinic.dto.ClinicDtos.RescheduleRequest;
import com.nexora.clinic.dto.ClinicDtos.SpecialtyDto;
import com.nexora.clinic.dto.ClinicDtos.StatusChangeRequest;
import com.nexora.clinic.entity.AppointmentStatus;
import com.nexora.clinic.facade.PatientAppointmentFacade;
import com.nexora.clinic.service.AppointmentQueryService;
import com.nexora.clinic.service.DoctorService;
import com.nexora.clinic.service.PatientService;
import com.nexora.clinic.service.ScheduleService;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.command.SimpleCommand;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.CurrentUser;
import com.nexora.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clinic")
public class ClinicController {

    private final PatientService patients;
    private final DoctorService doctors;
    private final ScheduleService schedule;
    private final AppointmentQueryService appointmentQueries;
    private final PatientAppointmentFacade facade;
    private final CommandInvoker invoker;

    public ClinicController(PatientService patients, DoctorService doctors, ScheduleService schedule,
                            AppointmentQueryService appointmentQueries, PatientAppointmentFacade facade,
                            CommandInvoker invoker) {
        this.patients = patients;
        this.doctors = doctors;
        this.schedule = schedule;
        this.appointmentQueries = appointmentQueries;
        this.facade = facade;
        this.invoker = invoker;
    }

    // ------------------------------------------------------------------ patients
    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','DOCTOR')")
    public PageResponse<PatientDto> patients(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String sort) {
        return patients.search(q, page, size, sort);
    }

    @GetMapping("/patients/me")
    public PatientDto myPatientRecord() {
        Long userId = CurrentUser.require().id();
        return patients.forUser(userId).orElseThrow(() -> new NotFoundException("Patient", "user " + userId));
    }

    @GetMapping("/patients/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','DOCTOR')")
    public PatientDto patient(@PathVariable Long id) {
        return patients.get(id);
    }

    @PostMapping("/patients")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PatientDto createPatient(@Valid @RequestBody PatientRequest request) {
        return invoker.run(SimpleCommand.of("CREATE_PATIENT", "Patient", () -> patients.create(request),
                PatientDto::id, request.firstName() + " " + request.lastName()));
    }

    @PutMapping("/patients/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PatientDto updatePatient(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return invoker.run(SimpleCommand.of("UPDATE_PATIENT", "Patient", () -> patients.update(id, request),
                PatientDto::id, "Пациент картасы жаңартылды"));
    }

    // ------------------------------------------------------------------ doctors
    @GetMapping("/specialties")
    public List<SpecialtyDto> specialties() {
        return doctors.specialties();
    }

    @GetMapping("/doctors")
    public List<DoctorDto> doctors(@RequestParam(required = false) Long specialtyId) {
        return doctors.list(specialtyId);
    }

    @GetMapping("/doctors/{id}")
    public DoctorDto doctor(@PathVariable Long id) {
        return doctors.get(id);
    }

    @GetMapping("/doctors/{id}/slots")
    public DaySlots slots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return schedule.slots(id, date);
    }

    // ------------------------------------------------------------------ appointments
    @GetMapping("/appointments")
    public PageResponse<AppointmentDto> appointments(@RequestParam(required = false) Long doctorId,
                                                     @RequestParam(required = false) Long patientId,
                                                     @RequestParam(required = false) Long specialtyId,
                                                     @RequestParam(required = false) AppointmentStatus status,
                                                     @RequestParam(required = false) Instant from,
                                                     @RequestParam(required = false) Instant to,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String sort) {
        return appointmentQueries.search(new AppointmentQueryService.AppointmentFilter(doctorId, patientId, specialtyId,
                status, from, to, q), CurrentUser.require(), page, size, sort);
    }

    @GetMapping("/appointments/{id}")
    public AppointmentDto appointment(@PathVariable Long id) {
        return appointmentQueries.get(id, CurrentUser.require());
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','USER')")
    public AppointmentDto book(@Valid @RequestBody BookAppointmentRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("BOOK_APPOINTMENT", "Appointment", () -> facade.book(request, user),
                AppointmentDto::id, "дәрігер " + request.doctorId() + ", пациент " + request.patientId() + ", уақыты " + request.startTime()));
    }

    @PostMapping("/appointments/{id}/reschedule")
    public AppointmentDto reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("RESCHEDULE_APPOINTMENT", "Appointment",
                () -> facade.reschedule(id, request.startTime(), user), AppointmentDto::id, "жаңа уақыт " + request.startTime()));
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("CANCEL_APPOINTMENT", "Appointment",
                () -> facade.cancel(id, request.reason(), user), AppointmentDto::id, request.reason()));
    }

    @PostMapping("/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE','DOCTOR')")
    public AppointmentDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("SET_APPOINTMENT_STATUS", "Appointment",
                () -> facade.changeStatus(id, request, user), AppointmentDto::id, String.valueOf(request.status())));
    }
}
