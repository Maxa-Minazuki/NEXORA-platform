package com.nexora.integration;

import com.nexora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scenarios 6-8: search sorted by price, confirm and cancel, forbidden transition. */
class FlightScenarioTest extends IntegrationTestBase {

    private final String tomorrow = LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(2).toString();

    @Test
    void searchSortedByPriceAndByDuration() throws Exception {
        var byPrice = read(getAs("user", "/api/flights/search?from=ALA&to=NQZ&date=" + tomorrow + "&passengers=1&sort=price")
                .andExpect(status().isOk()));
        List<BigDecimal> prices = new ArrayList<>();
        byPrice.get("flights").forEach(f -> prices.add(new BigDecimal(f.get("price").asText())));
        assertThat(prices).hasSizeGreaterThanOrEqualTo(2).isSorted();
        assertThat(byPrice.get("disclaimer").asText()).contains("Оқу демонстрациясы");

        var byDuration = read(getAs("user", "/api/flights/search?from=ALA&to=NQZ&date=" + tomorrow + "&sort=duration"));
        List<Integer> durations = new ArrayList<>();
        byDuration.get("flights").forEach(f -> durations.add(f.get("durationMinutes").asInt()));
        assertThat(durations).isSorted();
        assertThat(byDuration.get("flights").get(0).get("flightNumber").asText())
                .isNotEqualTo(byPrice.get("flights").get(0).get("flightNumber").asText());
    }

    private long firstFlight() throws Exception {
        return read(getAs("user", "/api/flights/search?from=ALA&to=NQZ&date=" + tomorrow + "&sort=departure"))
                .get("flights").get(0).get("id").asLong();
    }

    @Test
    void confirmIssuesDocumentAndCancelReleasesSeats() throws Exception {
        long flightId = firstFlight();
        int seatsBefore = read(getAs("user", "/api/flights/" + flightId)).get("availableSeats").asInt();
        var booking = read(postAs("user", "/api/bookings", Map.of("flightId", flightId, "contactEmail", "user@test.local",
                "passengers", List.of(Map.of("fullName", "Test Passenger", "seatNumber", "10F"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")));
        long id = booking.get("id").asLong();
        assertThat(read(getAs("user", "/api/flights/" + flightId)).get("availableSeats").asInt()).isEqualTo(seatsBefore - 1);

        var confirmed = read(postAs("user", "/api/bookings/" + id + "/confirm", null).andExpect(status().isOk()));
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.get("confirmation").get("documentNumber").asText()).isEqualTo("CNF-" + booking.get("reference").asText());
        long docId = confirmed.get("confirmation").get("workflowDocumentId").asLong();
        getAs("user", "/api/documents/" + docId).andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("APPROVED"))
                .andExpect(jsonPath("$.summary.type").value("FLIGHT_BOOKING_CONFIRMATION"));

        postAs("user", "/api/bookings/" + id + "/cancel", Map.of("reason", "Plans changed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(read(getAs("user", "/api/flights/" + flightId)).get("availableSeats").asInt()).isEqualTo(seatsBefore);
    }

    @Test
    void cancelledBookingCannotBeConfirmed() throws Exception {
        long flightId = firstFlight();
        var booking = read(postAs("user", "/api/bookings", Map.of("flightId", flightId, "contactEmail", "user@test.local",
                "passengers", List.of(Map.of("fullName", "Second Passenger", "seatNumber", "")))));
        long id = booking.get("id").asLong();
        postAs("user", "/api/bookings/" + id + "/cancel", Map.of("reason", "test")).andExpect(status().isOk());
        postAs("user", "/api/bookings/" + id + "/confirm", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void sameSeatCannotBeBookedTwice() throws Exception {
        long flightId = firstFlight();
        Map<String, Object> body = Map.of("flightId", flightId, "contactEmail", "user@test.local",
                "passengers", List.of(Map.of("fullName", "Seat Owner", "seatNumber", "9A")));
        postAs("user", "/api/bookings", body).andExpect(status().isCreated());
        postAs("manager", "/api/bookings", body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_TAKEN"));
    }

    @Test
    void otherUsersCannotSeeOrCancelMyBooking() throws Exception {
        long flightId = firstFlight();
        var booking = read(postAs("user", "/api/bookings", Map.of("flightId", flightId, "contactEmail", "user@test.local",
                "passengers", List.of(Map.of("fullName", "Private", "seatNumber", "")))));
        long id = booking.get("id").asLong();
        getAs("employee", "/api/bookings/" + id).andExpect(status().isNotFound());
        postAs("employee", "/api/bookings/" + id + "/cancel", Map.of("reason", "not mine")).andExpect(status().isNotFound());
    }
}
