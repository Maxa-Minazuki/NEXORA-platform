package com.nexora.integration;

import com.nexora.clinic.dto.ClinicDtos.BookAppointmentRequest;
import com.nexora.clinic.facade.PatientAppointmentFacade;
import com.nexora.shared.exception.ConflictException;
import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scenarios 4-5: booking a free slot, rejecting overlaps (also under concurrency). */
class ClinicScenarioTest extends IntegrationTestBase {

    @Autowired
    PatientAppointmentFacade facade;

    private static LocalDate nextWeekday(int daysAhead) {
        LocalDate d = LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(daysAhead);
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    private List<String> freeSlots(long doctorId, LocalDate date) throws Exception {
        var slots = read(getAs("employee", "/api/clinic/doctors/" + doctorId + "/slots?date=" + date));
        List<String> free = new ArrayList<>();
        slots.get("slots").forEach(s -> { if (s.get("available").asBoolean()) free.add(s.get("start").asText()); });
        return free;
    }

    @Test
    void bookFreeSlotAndRejectOverlap() throws Exception {
        LocalDate date = nextWeekday(8);
        String slot = freeSlots(4, date).getFirst();
        postAs("employee", "/api/clinic/appointments", Map.of("patientId", 3, "doctorId", 4, "startTime", slot,
                "reason", "Integration test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.doctorId").value(4));
        postAs("employee", "/api/clinic/appointments", Map.of("patientId", 5, "doctorId", 4, "startTime", slot,
                "reason", "Second patient, same slot"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
        assertThat(freeSlots(4, date)).doesNotContain(slot);
    }

    @Test
    void outsideWorkingHoursIsRejected() throws Exception {
        LocalDate date = nextWeekday(9);
        Instant evening = date.atTime(20, 0).atZone(ZoneId.of("Asia/Almaty")).toInstant();
        postAs("employee", "/api/clinic/appointments", Map.of("patientId", 3, "doctorId", 4, "startTime",
                evening.toString(), "reason", "Too late")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OUTSIDE_WORKING_HOURS"));
    }

    @Test
    void concurrentRequestsForTheSameSlotYieldExactlyOneBooking() throws Exception {
        LocalDate date = nextWeekday(10);
        Instant slot = Instant.parse(freeSlots(6, date).get(2));
        var staff = principal("employee");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            long patient = 6 + i;
            tasks.add(() -> {
                try {
                    facade.book(new BookAppointmentRequest(patient, 6L, slot, "race"), staff);
                    return true;
                } catch (ConflictException ex) {
                    return false;
                }
            });
        }
        int successes = 0;
        for (Future<Boolean> f : pool.invokeAll(tasks)) {
            if (f.get()) {
                successes++;
            }
        }
        pool.shutdown();
        assertThat(successes).isEqualTo(1);
    }

    @Test
    void cancelAndForbiddenStatusTransition() throws Exception {
        LocalDate date = nextWeekday(11);
        String slot = freeSlots(2, date).getFirst();
        var appt = read(postAs("employee", "/api/clinic/appointments", Map.of("patientId", 4, "doctorId", 2,
                "startTime", slot, "reason", "Cancel test")));
        long id = appt.get("id").asLong();
        postAs("employee", "/api/clinic/appointments/" + id + "/cancel", Map.of("reason", "Patient request"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        postAs("employee", "/api/clinic/appointments/" + id + "/status", Map.of("status", "CONFIRMED"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }
}
