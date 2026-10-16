package com.nexora.patterns;

import com.nexora.flight.entity.Flight;
import com.nexora.flight.strategy.CheapestFirstStrategy;
import com.nexora.flight.strategy.EarliestDepartureStrategy;
import com.nexora.flight.strategy.FlightSortStrategyResolver;
import com.nexora.flight.strategy.ShortestDurationStrategy;
import com.nexora.inventory.entity.Category;
import com.nexora.inventory.entity.Product;
import com.nexora.inventory.entity.ReplenishmentPolicy;
import com.nexora.inventory.repository.InventoryTransactionRepository;
import com.nexora.inventory.strategy.DemandBasedStrategy;
import com.nexora.inventory.strategy.FixedLotStrategy;
import com.nexora.inventory.strategy.InventoryStrategyResolver;
import com.nexora.inventory.strategy.MinimumLevelStrategy;
import com.nexora.inventory.strategy.SafetyBufferStrategy;
import com.nexora.inventory.strategy.TopUpToTargetStrategy;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.NotificationPreference;
import com.nexora.notification.entity.Severity;
import com.nexora.notification.strategy.ChannelSelector;
import com.nexora.notification.strategy.CriticalEscalationChannelStrategy;
import com.nexora.notification.strategy.PreferenceBasedChannelStrategy;
import com.nexora.shared.exception.BusinessRuleException;
import com.nexora.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Strategy pattern: interchangeable algorithms change the actual result. */
class StrategyPatternTest {

    private static Flight flight(String number, String price, int minutes, int departureHour) {
        Flight f = mock(Flight.class);
        Instant dep = Instant.parse("2026-10-20T00:00:00Z").plus(Duration.ofHours(departureHour));
        when(f.getFlightNumber()).thenReturn(number);
        when(f.getBasePrice()).thenReturn(new BigDecimal(price));
        when(f.getDepartureTime()).thenReturn(dep);
        when(f.duration()).thenReturn(Duration.ofMinutes(minutes));
        return f;
    }

    @Test
    void flightSortStrategiesProduceDifferentOrders() {
        List<Flight> flights = List.of(flight("EARLY", "41800", 95, 2), flight("CHEAP", "27900", 120, 14),
                flight("MID", "32500", 105, 7));
        var resolver = new FlightSortStrategyResolver(List.of(new CheapestFirstStrategy(), new ShortestDurationStrategy(),
                new EarliestDepartureStrategy()));
        assertThat(order(flights, resolver, "price")).containsExactly("CHEAP", "MID", "EARLY");
        assertThat(order(flights, resolver, "duration")).containsExactly("EARLY", "MID", "CHEAP");
        assertThat(order(flights, resolver, "departure")).containsExactly("EARLY", "MID", "CHEAP");
        assertThat(resolver.resolve(null).key()).isEqualTo("price");
        assertThatThrownBy(() -> resolver.resolve("random")).isInstanceOf(BusinessRuleException.class);
    }

    private static List<String> order(List<Flight> flights, FlightSortStrategyResolver resolver, String key) {
        List<Flight> copy = new ArrayList<>(flights);
        copy.sort(resolver.resolve(key).comparator());
        return copy.stream().map(Flight::getFlightNumber).toList();
    }

    private static Product product(int qty, int min, ReplenishmentPolicy policy) {
        Product p = mock(Product.class);
        when(p.getId()).thenReturn(7L);
        when(p.getQuantity()).thenReturn(qty);
        when(p.getMinStock()).thenReturn(min);
        when(p.getReplenishmentPolicy()).thenReturn(policy);
        return p;
    }

    @Test
    void replenishmentStrategiesComputeDifferentQuantities() {
        InventoryTransactionRepository repo = mock(InventoryTransactionRepository.class);
        when(repo.outflowSince(eq(7L), any())).thenReturn(90L); // 3 units/day
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC);
        Product p = product(5, 20, ReplenishmentPolicy.TOP_UP_TO_TARGET);

        assertThat(new TopUpToTargetStrategy().suggestQuantity(p)).isEqualTo(55);      // 60 - 5
        assertThat(new FixedLotStrategy().suggestQuantity(p)).isEqualTo(50);           // need 35 -> one lot
        assertThat(new DemandBasedStrategy(repo, clock).suggestQuantity(p)).isEqualTo(78); // 63 + 20 - 5
    }

    @Test
    void lowStockDetectionStrategiesDiffer() {
        var minimum = new MinimumLevelStrategy();
        var buffer = new SafetyBufferStrategy();
        assertThat(minimum.isCritical(11, 10)).isFalse();
        assertThat(buffer.isCritical(11, 10)).isTrue();   // within the 25% buffer (threshold 13)
        assertThat(buffer.criticalThreshold(10)).isEqualTo(13);

        var resolver = new InventoryStrategyResolver(List.of(minimum, buffer),
                List.of(new TopUpToTargetStrategy(), new FixedLotStrategy()), TestFixtures.settings("safety-buffer", false));
        assertThat(resolver.activeDetection()).isSameAs(buffer);
        assertThatThrownBy(() -> new InventoryStrategyResolver(List.of(minimum), List.of(),
                TestFixtures.settings("unknown", false))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void channelStrategyRespectsPreferencesUnlessCritical() {
        var selector = new ChannelSelector(List.of(new PreferenceBasedChannelStrategy(), new CriticalEscalationChannelStrategy()));
        Map<NotificationChannel, NotificationPreference> prefs = Map.of(
                NotificationChannel.IN_APP, new NotificationPreference(1L, NotificationChannel.IN_APP, false, Severity.INFO),
                NotificationChannel.EMAIL, new NotificationPreference(1L, NotificationChannel.EMAIL, true, Severity.WARNING));

        assertThat(selector.strategyFor(Severity.INFO).select(Severity.INFO, prefs)).isEmpty();
        assertThat(selector.strategyFor(Severity.WARNING).select(Severity.WARNING, prefs))
                .containsExactly(NotificationChannel.EMAIL);
        assertThat(selector.strategyFor(Severity.CRITICAL).name()).isEqualTo("CriticalEscalation");
        assertThat(selector.strategyFor(Severity.CRITICAL).select(Severity.CRITICAL, prefs))
                .containsExactly(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }

    @SuppressWarnings("unused")
    private static Category category() {
        return new Category("Test", null);
    }
}
