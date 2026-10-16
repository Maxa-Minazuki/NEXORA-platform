package com.nexora.flight.mediator;

import com.nexora.flight.entity.FlightBooking;
import com.nexora.flight.entity.FlightSeat;
import com.nexora.flight.repository.FlightBookingRepository;
import com.nexora.shared.config.PlatformSettings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;

/** Colleague: persists the PENDING booking with a unique reference and its passengers. */
@Component
public class BookingRecorder extends BookingColleague {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FlightBookingRepository bookings;
    private final PlatformSettings settings;

    public BookingRecorder(FlightBookingRepository bookings, PlatformSettings settings) {
        this.bookings = bookings;
        this.settings = settings;
    }

    @Override public String role() { return "BookingRecorder"; }

    void record(BookingContext ctx) {
        BigDecimal total = ctx.flight.getBasePrice().multiply(BigDecimal.valueOf(ctx.passengers.size()));
        FlightBooking booking = new FlightBooking(newReference(), ctx.flight, ctx.userId, total, ctx.contactEmail,
                ctx.now.plus(settings.bookingHold()));
        for (int i = 0; i < ctx.passengers.size(); i++) {
            booking.addPassenger(ctx.passengers.get(i).fullName().trim(), ctx.seats.get(i).getSeatNumber());
        }
        bookings.saveAndFlush(booking);
        ctx.seats.forEach((FlightSeat s) -> s.assignTo(booking.getId()));
        ctx.booking = booking;
        ctx.log(role() + ": " + booking.getReference() + " брондауы «Күтуде» күйінде сақталды, ұстау мерзімі "
                + settings.bookingHold().toMinutes() + " мин");
        report(BookingSignal.BOOKING_RECORDED, ctx);
    }

    String newReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
            }
            if (!bookings.existsByReference(sb.toString())) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("Бірегей брондау кодын жасау мүмкін болмады");
    }
}
