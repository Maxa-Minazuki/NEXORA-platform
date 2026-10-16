package com.nexora.notification.controller;

import com.nexora.notification.dto.NotificationDtos.AttemptDto;
import com.nexora.notification.dto.NotificationDtos.ChannelInfo;
import com.nexora.notification.dto.NotificationDtos.DispatchReport;
import com.nexora.notification.dto.NotificationDtos.NotificationDraft;
import com.nexora.notification.dto.NotificationDtos.NotificationDto;
import com.nexora.notification.dto.NotificationDtos.PreferenceDto;
import com.nexora.notification.dto.NotificationDtos.PreferencesRequest;
import com.nexora.notification.dto.NotificationDtos.TestNotificationRequest;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.Severity;
import com.nexora.notification.realtime.SseHub;
import com.nexora.notification.service.NotificationDispatcher;
import com.nexora.notification.service.NotificationService;
import com.nexora.shared.command.CommandInvoker;
import com.nexora.shared.command.SimpleCommand;
import com.nexora.shared.security.AuthenticatedUser;
import com.nexora.shared.security.CurrentUser;
import com.nexora.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationDispatcher dispatcher;
    private final SseHub hub;
    private final CommandInvoker invoker;

    public NotificationController(NotificationService service, NotificationDispatcher dispatcher, SseHub hub,
                                  CommandInvoker invoker) {
        this.service = service;
        this.dispatcher = dispatcher;
        this.hub = hub;
        this.invoker = invoker;
    }

    @GetMapping
    public PageResponse<NotificationDto> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                              @RequestParam(required = false) Severity severity,
                                              @RequestParam(required = false) NotificationCategory category,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(CurrentUser.require().id(), unreadOnly, severity, category, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCount(CurrentUser.require().id()));
    }

    @GetMapping("/latest")
    public List<NotificationDto> latest() {
        return service.latest(CurrentUser.require().id());
    }

    @PostMapping("/{id}/read")
    public NotificationDto read(@PathVariable Long id) {
        return service.markRead(CurrentUser.require().id(), id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> readAll() {
        return Map.of("updated", service.markAllRead(CurrentUser.require().id()));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return hub.subscribe(CurrentUser.require().id());
    }

    @GetMapping("/preferences")
    public List<PreferenceDto> preferences() {
        return service.preferences(CurrentUser.require().id());
    }

    @PutMapping("/preferences")
    public List<PreferenceDto> updatePreferences(@Valid @RequestBody PreferencesRequest request) {
        Long userId = CurrentUser.require().id();
        return invoker.run(SimpleCommand.of("UPDATE_NOTIFICATION_PREFERENCES", "User",
                () -> service.updatePreferences(userId, request.preferences()), r -> userId, request.preferences().toString()));
    }

    @GetMapping("/channels")
    public List<ChannelInfo> channels() {
        return service.channels(CurrentUser.require().id());
    }

    @GetMapping("/deliveries")
    public PageResponse<AttemptDto> deliveries(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.deliveries(CurrentUser.require(), page, size);
    }

    @PostMapping("/deliveries/{id}/retry")
    public AttemptDto retry(@PathVariable Long id) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("RETRY_DELIVERY", "DeliveryAttempt", () -> service.retry(user, id),
                AttemptDto::id, "жеткізуді қайталау #" + id));
    }

    /** Sends a notification to yourself: shows which Strategy picked which channels. */
    @PostMapping("/test")
    public DispatchReport test(@Valid @RequestBody TestNotificationRequest request) {
        AuthenticatedUser user = CurrentUser.require();
        return invoker.run(SimpleCommand.of("SEND_TEST_NOTIFICATION", "Notification",
                () -> dispatcher.dispatch(List.of(user.id()), new NotificationDraft(NotificationCategory.SYSTEM,
                        request.severity(), request.title(), request.message(), "/notifications", "SYSTEM")).getFirst(),
                DispatchReport::notificationId, request.severity().name()));
    }
}
