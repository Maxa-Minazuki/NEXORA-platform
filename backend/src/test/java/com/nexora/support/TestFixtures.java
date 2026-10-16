package com.nexora.support;

import com.nexora.shared.config.NexoraProperties;
import com.nexora.shared.config.PlatformSettings;

import java.math.BigDecimal;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static PlatformSettings settings() {
        return settings("minimum-level", false);
    }

    public static PlatformSettings settings(String lowStockPolicy, boolean emailEnabled) {
        return new PlatformSettings(new NexoraProperties("NEXORA", "Asia/Almaty", 15, 30,
                new BigDecimal("250000.00"), new BigDecimal("1500000.00"), lowStockPolicy, 14, emailEnabled,
                "no-reply@test.local"));
    }
}
