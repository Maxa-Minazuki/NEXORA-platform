package com.nexora.patterns;

import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.flight.state.BookingStateRegistry;
import com.nexora.flight.state.CancelledBookingState;
import com.nexora.flight.state.ConfirmedBookingState;
import com.nexora.flight.state.ExpiredBookingState;
import com.nexora.flight.state.PendingBookingState;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** State pattern: legal and illegal transitions of a flight booking. */
class BookingStatePatternTest {

    private final BookingStateRegistry registry = new BookingStateRegistry(List.of(new PendingBookingState(),
            new ConfirmedBookingState(), new CancelledBookingState(), new ExpiredBookingState()));
    private final Instant now = Instant.parse("2026-10-16T10:00:00Z");

    private FlightBooking booking(Instant departure) {
        Flight flight = mock(Flight.class);
        when(flight.getDepartureTime()).thenReturn(departure);
        return new FlightBooking("ABC234", flight, 1L, new BigDecimal("1000.00"), "a@b.c", now.plus(Duration.ofMinutes(15)));
    }

    @Test
    void pendingBookingCanBeConfirmed() {
        FlightBooking b = booking(now.plus(Duration.ofDays(2)));
        registry.of(b).confirm(b, now);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.getConfirmedAt()).isEqualTo(now);
        assertThat(registry.of(b).allowedActions()).containsExactly("cancel");
    }

    @Test
    void pendingBookingCannotBeConfirmedAfterHoldExpired() {
        FlightBooking b = booking(now.plus(Duration.ofDays(2)));
        assertThatThrownBy(() -> registry.of(b).confirm(b, now.plus(Duration.ofMinutes(16))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("мерзімі өтті");
        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void cancelledBookingCannotBeConfirmedAgain() {
        FlightBooking b = booking(now.plus(Duration.ofDays(2)));
        registry.of(b).cancel(b, "changed plans", now);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> registry.of(b).confirm(b, now))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("«Болдырылмаған» күйіндегі брондауды растау мүмкін емес");
        assertThat(registry.of(b).allowedActions()).isEmpty();
    }

    @Test
    void confirmedBookingCannotBeCancelledCloseToDeparture() {
        FlightBooking b = booking(now.plus(Duration.ofMinutes(90)));
        registry.of(b).confirm(b, now);
        assertThatThrownBy(() -> registry.of(b).cancel(b, "late", now))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("2 сағат");
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void expiredIsTerminal() {
        FlightBooking b = booking(now.plus(Duration.ofDays(2)));
        registry.of(b).expire(b, now);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThatThrownBy(() -> registry.of(b).cancel(b, "x", now)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> registry.of(b).expire(b, now)).isInstanceOf(InvalidStateTransitionException.class);
    }
}
