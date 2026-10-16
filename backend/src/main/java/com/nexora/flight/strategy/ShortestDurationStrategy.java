package com.nexora.flight.strategy;

import com.nexora.flight.entity.Flight;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ShortestDurationStrategy implements FlightSortStrategy {
    @Override public String key() { return "duration"; }
    @Override public String label() { return "Ең қысқа ұзақтық"; }
    @Override public Comparator<Flight> comparator() {
        return Comparator.comparing(Flight::duration).thenComparing(Flight::getBasePrice);
    }
}
