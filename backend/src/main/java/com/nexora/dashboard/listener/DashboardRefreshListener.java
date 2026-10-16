package com.nexora.dashboard.listener;

import com.nexora.notification.realtime.SseHub;
import com.nexora.shared.event.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/** Observer: tells every open dashboard to refetch its (database-backed) metrics. */
@Component
public class DashboardRefreshListener {

    private final SseHub hub;

    public DashboardRefreshListener(SseHub hub) {
        this.hub = hub;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DomainEvent event) {
        hub.broadcast("dashboard", Map.of("module", event.module(), "event", event.getClass().getSimpleName(),
                "summary", event.summary(), "at", event.occurredAt().toString()));
    }
}
