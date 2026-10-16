package com.nexora.flight.service;

import com.nexora.flight.facade.BookingFacade;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.command.SimpleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Moves PENDING bookings whose hold has lapsed to EXPIRED (via the State pattern) and frees their seats. */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final com.nexora.flight.repository.FlightBookingRepository bookings;
    private final BookingFacade facade;
    private final CommandInvoker invoker;
    private final Clock clock;

    public BookingExpiryJob(com.nexora.flight.repository.FlightBookingRepository bookings, BookingFacade facade,
                            CommandInvoker invoker, Clock clock) {
        this.bookings = bookings;
        this.facade = facade;
        this.invoker = invoker;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${nexora.booking-expiry-check-ms:60000}", initialDelay = 20000)
    public void expireLapsedHolds() {
        for (Long id : bookings.findExpiredPendingIds(clock.instant())) {
            try {
                invoker.run(SimpleCommand.of("EXPIRE_FLIGHT_BOOKING", "FlightBooking", () -> facade.expire(id),
                        r -> id, "Орындарды ұстау мерзімі өтті"));
            } catch (RuntimeException ex) {
                log.warn("Could not expire booking {}: {}", id, ex.getMessage());
            }
        }
    }
}
