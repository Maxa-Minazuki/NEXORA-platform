package com.nexora.flight.facade;

import com.nexora.flight.dto.FlightDtos.AirportDto;
import com.nexora.flight.dto.FlightDtos.BookingDto;
import com.nexora.flight.dto.FlightDtos.CreateBookingRequest;
import com.nexora.flight.dto.FlightDtos.FlightDto;
import com.nexora.flight.dto.FlightDtos.SearchResult;
import com.nexora.flight.dto.FlightDtos.SeatDto;
import com.nexora.flight.entity.Airport;
import com.nexora.flight.entity.BookingStatus;
import com.nexora.flight.entity.Flight;
import com.nexora.flight.entity.FlightBooking;
import com.nexora.flight.mediator.BookingContext;
import com.nexora.flight.mediator.BookingMediator;
import com.nexora.flight.repository.AirportRepository;
import com.nexora.flight.repository.BookingDocumentRepository;
import com.nexora.flight.repository.FlightBookingRepository;
import com.nexora.flight.repository.FlightRepository;
import com.nexora.flight.repository.FlightSeatRepository;
import com.nexora.flight.service.FlightMapper;
import com.nexora.flight.state.BookingStateRegistry;
import com.nexora.flight.strategy.FlightSortStrategy;
import com.nexora.flight.strategy.FlightSortStrategyResolver;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.shared.exception.NotFoundException;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.Role;
import com.nexora.shared.web.PageRequests;
import com.nexora.shared.web.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * <h2>Design pattern: Facade</h2>
 *
 * The single, simple API of the flight subsystem used by the controller: search, seat map, create,
 * confirm, cancel, expire, history. Behind it sit the sort Strategies, the State registry, the
 * {@link BookingMediator} and its colleagues, the repositories and the confirmation document port.
 * The facade adds access control and DTO mapping; the step-by-step cooperation is delegated to the
 * mediator.
 */
@Service
public class BookingFacade {

    public static final String DISCLAIMER = "Оқу демонстрациясы: рейстер, бағалар мен брондаулар ойдан шығарылған. "
            + "Әуе компаниясында нақты брондау немесе төлем жүргізілмейді.";

    private final AirportRepository airports;
    private final FlightRepository flights;
    private final FlightSeatRepository seats;
    private final FlightBookingRepository bookings;
    private final BookingDocumentRepository bookingDocuments;
    private final FlightSortStrategyResolver sortStrategies;
    private final BookingStateRegistry states;
    private final BookingMediator mediator;
    private final Clock clock;

    public BookingFacade(AirportRepository airports, FlightRepository flights, FlightSeatRepository seats,
                         FlightBookingRepository bookings, BookingDocumentRepository bookingDocuments,
                         FlightSortStrategyResolver sortStrategies, BookingStateRegistry states,
                         BookingMediator mediator, Clock clock) {
        this.airports = airports;
        this.flights = flights;
        this.seats = seats;
        this.bookings = bookings;
        this.bookingDocuments = bookingDocuments;
        this.sortStrategies = sortStrategies;
        this.states = states;
        this.mediator = mediator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AirportDto> airports() {
        return airports.findAllByOrderByCityAsc().stream().map(FlightMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SearchResult search(String originCode, String destinationCode, LocalDate date, int passengers, String sort) {
        if (passengers < 1 || passengers > 6) {
            throw new BusinessRuleException("INVALID_PASSENGERS", "Жолаушылар саны 1-ден 6-ға дейін болуы керек");
        }
        if (originCode.equalsIgnoreCase(destinationCode)) {
            throw new BusinessRuleException("SAME_AIRPORT", "Ұшу және келу әуежайлары әртүрлі болуы керек");
        }
        FlightSortStrategy strategy = sortStrategies.resolve(sort);
        Airport origin = airports.findByCodeIgnoreCase(originCode).orElseThrow(() -> new NotFoundException("Airport", originCode));
        Airport destination = airports.findByCodeIgnoreCase(destinationCode)
                .orElseThrow(() -> new NotFoundException("Airport", destinationCode));
        ZoneId zone = origin.zone();
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<FlightDto> result = flights.search(origin.getId(), destination.getId(), from, to, clock.instant(), passengers)
                .stream().sorted(strategy.comparator()).map(FlightMapper::toDto).toList();
        return new SearchResult(strategy.key(), strategy.label(),
                sortStrategies.all().stream().map(FlightSortStrategy::key).toList(), result, DISCLAIMER);
    }

    @Transactional(readOnly = true)
    public FlightDto flight(Long id) {
        return FlightMapper.toDto(flights.findDetailedById(id).orElseThrow(() -> new NotFoundException("Flight", id)));
    }

    @Transactional(readOnly = true)
    public List<SeatDto> seatMap(Long flightId) {
        return seats.findByFlightIdOrderByIdAsc(flightId).stream()
                .map(s -> new SeatDto(s.getSeatNumber(), s.getStatus().name())).toList();
    }

    @Transactional
    public BookingDto create(CreateBookingRequest request, AuthenticatedUser user) {
        BookingContext ctx = mediator.create(BookingContext.forCreate(request.flightId(), user.id(), request.passengers(),
                request.contactEmail(), clock.instant()));
        return toDto(ctx.booking(), ctx.log());
    }

    @Transactional
    public BookingDto confirm(Long bookingId, AuthenticatedUser user) {
        FlightBooking booking = lockOwned(bookingId, user);
        BookingContext ctx = mediator.confirm(BookingContext.forExisting(booking, null, clock.instant()));
        return toDto(ctx.booking(), ctx.log());
    }

    @Transactional
    public BookingDto cancel(Long bookingId, String reason, AuthenticatedUser user) {
        FlightBooking booking = lockOwned(bookingId, user);
        BookingContext ctx = mediator.cancel(BookingContext.forExisting(booking, reason.trim(), clock.instant()));
        return toDto(ctx.booking(), ctx.log());
    }

    /** Invoked by the expiry job (system actor). Skips bookings that were confirmed in the meantime. */
    @Transactional
    public boolean expire(Long bookingId) {
        FlightBooking booking = bookings.lockById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.PENDING || !clock.instant().isAfter(booking.getExpiresAt())) {
            return false;
        }
        flights.lockById(booking.getFlight().getId());
        mediator.expire(BookingContext.forExisting(booking, null, clock.instant()));
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingDto> history(AuthenticatedUser user, BookingStatus status, String q, int page, int size,
                                            boolean all) {
        Long userFilter = all && canSeeAll(user) ? null : user.id();
        var pageable = PageRequests.of(page, size, null, Set.of(), "-createdAt");
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(bookings.search(userFilter, status, query, pageable), b -> toDto(b, List.of()));
    }

    @Transactional(readOnly = true)
    public BookingDto get(Long id, AuthenticatedUser user) {
        FlightBooking b = bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking", id));
        if (!b.getUserId().equals(user.id()) && !canSeeAll(user)) {
            throw new NotFoundException("Booking", id);
        }
        return toDto(b, List.of());
    }

    private FlightBooking lockOwned(Long id, AuthenticatedUser user) {
        FlightBooking b = bookings.lockById(id).orElseThrow(() -> new NotFoundException("Booking", id));
        if (!b.getUserId().equals(user.id()) && !user.hasRole(Role.ADMIN, Role.MANAGER)) {
            throw new NotFoundException("Booking", id);
        }
        flights.lockById(b.getFlight().getId()); // seat counters change: serialize with concurrent bookings
        return b;
    }

    private static boolean canSeeAll(AuthenticatedUser user) {
        return user.hasRole(Role.ADMIN, Role.MANAGER);
    }

    private BookingDto toDto(FlightBooking b, List<String> log) {
        Flight f = b.getFlight();
        f.getOrigin().getCode();
        f.getDestination().getCode();
        return FlightMapper.toDto(b, states.of(b).allowedActions(), bookingDocuments.findByBookingId(b.getId()).orElse(null), log);
    }
}
