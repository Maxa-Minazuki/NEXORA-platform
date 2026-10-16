package com.nexora.flight.strategy;

import com.nexora.flight.entity.Flight;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class CheapestFirstStrategy implements FlightSortStrategy {
    @Override public String key() { return "price"; }
    @Override public String label() { return "Ең арзан баға"; }
    @Override public Comparator<Flight> comparator() {
        return Comparator.comparing(Flight::getBasePrice).thenComparing(Flight::getDepartureTime);
    }
}
