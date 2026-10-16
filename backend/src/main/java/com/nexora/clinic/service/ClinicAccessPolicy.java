package com.nexora.clinic.service;

import com.nexora.clinic.entity.Appointment;
import com.nexora.clinic.entity.Patient;
import com.nexora.clinic.repository.DoctorRepository;
import com.nexora.clinic.repository.PatientRepository;
import com.nexora.shared.exception.ForbiddenOperationException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Object-level authorization for clinic data: USERs only see and book their own patient record,
 * DOCTORs only manage appointments in their own calendar, staff roles see everything.
 */
@Component
public class ClinicAccessPolicy {

    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public ClinicAccessPolicy(PatientRepository patients, DoctorRepository doctors) {
        this.patients = patients;
        this.doctors = doctors;
    }

    public boolean isStaff(AuthenticatedUser user) {
        return user.hasRole(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE);
    }

    /** Patient id the user is restricted to, or empty when the user may see all patients. */
    public Optional<Long> restrictedPatientId(AuthenticatedUser user) {
        if (user.role() != Role.USER) {
            return Optional.empty();
        }
        return Optional.of(patients.findByUserId(user.id()).map(Patient::getId).orElse(-1L));
    }

    /** Doctor id the user is restricted to, or empty. */
    public Optional<Long> restrictedDoctorId(AuthenticatedUser user) {
        if (user.role() != Role.DOCTOR) {
            return Optional.empty();
        }
        return Optional.of(doctors.findByUserId(user.id()).map(d -> d.getId()).orElse(-1L));
    }

    public void checkCanBookFor(AuthenticatedUser user, Patient patient) {
        if (isStaff(user)) {
            return;
        }
        if (user.role() == Role.USER && user.id().equals(patient.getUserId())) {
            return;
        }
        throw new ForbiddenOperationException("Тек өз пациент картаңызға ғана жазыла аласыз");
    }

    public void checkCanModify(AuthenticatedUser user, Appointment appointment) {
        if (isStaff(user)) {
            return;
        }
        if (user.role() == Role.USER && user.id().equals(appointment.getPatient().getUserId())) {
            return;
        }
        if (user.role() == Role.DOCTOR && user.id().equals(appointment.getDoctor().getUserId())) {
            return;
        }
        throw new ForbiddenOperationException("Бұл қабылдауды өзгерте алмайсыз");
    }

    /** Clinical outcomes (completed / no-show / confirmed) are set by the doctor or clinic staff only. */
    public void checkCanSetOutcome(AuthenticatedUser user, Appointment appointment) {
        if (isStaff(user)) {
            return;
        }
        if (user.role() == Role.DOCTOR && user.id().equals(appointment.getDoctor().getUserId())) {
            return;
        }
        throw new ForbiddenOperationException("Бұл күйді тек емдеуші дәрігер немесе емхана қызметкері қоя алады");
    }
}
