package com.nexora.notification.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of open Server-Sent-Events connections per user. In-memory: live pushes are best effort
 * and do not survive restarts - the inbox in the database is the durable record, and the browser's
 * EventSource reconnects automatically.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("userId", userId)));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public int sessionCount(Long userId) {
        List<SseEmitter> list = userId == null ? null : emitters.get(userId);
        return list == null ? 0 : list.size();
    }

    public int totalSessions() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    public void sendToUser(Long userId, String event, Object data) {
        List<SseEmitter> list = userId == null ? null : emitters.get(userId);
        if (list != null) {
            list.forEach(e -> send(list, e, event, data));
        }
    }

    public void broadcast(String event, Object data) {
        emitters.values().forEach(list -> list.forEach(e -> send(list, e, event, data)));
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        broadcast("ping", Map.of("t", System.currentTimeMillis()));
    }

    private static void send(List<SseEmitter> list, SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException ex) {
            list.remove(emitter);
            log.debug("Dropped SSE connection: {}", ex.getMessage());
        }
    }
}
