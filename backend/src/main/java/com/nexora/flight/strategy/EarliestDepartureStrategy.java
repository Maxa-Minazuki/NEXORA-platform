package com.nexora.flight.strategy;

import com.nexora.flight.entity.Flight;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class EarliestDepartureStrategy implements FlightSortStrategy {
    @Override public String key() { return "departure"; }
    @Override public String label() { return "Ең ерте ұшу"; }
    @Override public Comparator<Flight> comparator() {
        return Comparator.comparing(Flight::getDepartureTime).thenComparing(Flight::getBasePrice);
    }
}
