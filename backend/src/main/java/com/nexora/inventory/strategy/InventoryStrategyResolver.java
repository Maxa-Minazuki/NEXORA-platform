package com.nexora.inventory.strategy;

import com.nexora.inventory.entity.ReplenishmentPolicy;
import com.nexora.shared.config.PlatformSettings;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Looks strategies up in maps built once from all Spring beans implementing the interfaces, so a new
 * strategy is added by writing one new {@code @Component} - no switch statement to edit.
 */
@Component
public class InventoryStrategyResolver {

    private final Map<String, LowStockDetectionStrategy> detection;
    private final Map<ReplenishmentPolicy, ReplenishmentStrategy> replenishment = new EnumMap<>(ReplenishmentPolicy.class);
    private final PlatformSettings settings;

    public InventoryStrategyResolver(List<LowStockDetectionStrategy> detectionStrategies,
                                     List<ReplenishmentStrategy> replenishmentStrategies,
                                     PlatformSettings settings) {
        this.detection = detectionStrategies.stream()
                .collect(Collectors.toMap(LowStockDetectionStrategy::key, Function.identity()));
        replenishmentStrategies.forEach(s -> replenishment.put(s.policy(), s));
        this.settings = settings;
        if (!detection.containsKey(settings.lowStockPolicy())) {
            throw new IllegalStateException("Unknown nexora.low-stock-policy: " + settings.lowStockPolicy()
                    + ", expected one of " + detection.keySet());
        }
    }

    /** Strategy selected by the platform-wide setting held in the PlatformSettings singleton. */
    public LowStockDetectionStrategy activeDetection() {
        return detection.get(settings.lowStockPolicy());
    }

    public LowStockDetectionStrategy detection(String key) {
        LowStockDetectionStrategy s = detection.get(key);
        if (s == null) {
            throw new IllegalArgumentException("Белгісіз төмен қор стратегиясы: " + key);
        }
        return s;
    }

    public ReplenishmentStrategy replenishment(ReplenishmentPolicy policy) {
        return replenishment.get(policy);
    }

    public List<ReplenishmentStrategy> allReplenishment() {
        return List.copyOf(replenishment.values());
    }

    public List<LowStockDetectionStrategy> allDetection() {
        return List.copyOf(detection.values());
    }
}
