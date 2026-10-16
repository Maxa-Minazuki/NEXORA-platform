package com.nexora.auth.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles brute-force attempts: at most {@value #MAX_FAILURES} failed logins per username+IP per
 * {@link #WINDOW}. In-memory and per instance, which is appropriate for this single-node demo.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(5);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Deque<Instant> q = failures.get(key);
        if (q == null) {
            return false;
        }
        synchronized (q) {
            prune(q);
            return q.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(clock.instant());
        }
    }

    public void recordSuccess(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> q) {
        Instant limit = clock.instant().minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().isBefore(limit)) {
            q.pollFirst();
        }
    }
}
