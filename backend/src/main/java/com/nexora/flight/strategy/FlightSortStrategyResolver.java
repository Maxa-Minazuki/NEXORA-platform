package com.nexora.flight.strategy;

import com.nexora.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FlightSortStrategyResolver {

    public static final String DEFAULT = "price";

    private final Map<String, FlightSortStrategy> byKey;

    public FlightSortStrategyResolver(List<FlightSortStrategy> strategies) {
        this.byKey = strategies.stream().collect(Collectors.toMap(FlightSortStrategy::key, Function.identity()));
    }

    public FlightSortStrategy resolve(String key) {
        FlightSortStrategy s = byKey.get(key == null || key.isBlank() ? DEFAULT : key);
        if (s == null) {
            throw new BusinessRuleException("UNKNOWN_SORT", "Белгісіз сұрыптау: '" + key + "'. Мүмкін мәндер: " + byKey.keySet());
        }
        return s;
    }

    public List<FlightSortStrategy> all() {
        return byKey.values().stream().sorted(java.util.Comparator.comparing(FlightSortStrategy::key)).toList();
    }
}
