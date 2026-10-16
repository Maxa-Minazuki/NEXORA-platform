package com.nexora.flight.strategy;

import com.nexora.flight.entity.Flight;

import java.util.Comparator;

/**
 * <h2>Design pattern: Strategy - flight result ordering</h2>
 *
 * The search endpoint receives {@code sort=price|duration|departure}; the facade looks the strategy
 * up by key and applies it. Adding "fewest stops" or "best value" means adding one class.
 */
public interface FlightSortStrategy {

    String key();

    String label();

    Comparator<Flight> comparator();
}
