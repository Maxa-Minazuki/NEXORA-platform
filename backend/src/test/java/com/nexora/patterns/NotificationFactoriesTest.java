package com.nexora.patterns;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.NotificationChannelFactory;
import com.nexora.notification.channel.email.EmailChannelFactory;
import com.nexora.notification.channel.inapp.InAppChannelFactory;
import com.nexora.notification.channel.inapp.InAppMessageSender;
import com.nexora.notification.delivery.EmailDeliveryHandlerCreator;
import com.nexora.notification.delivery.InAppDeliveryHandlerCreator;
import com.nexora.notification.entity.DeliveryAttempt;
import com.nexora.notification.entity.Notification;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.notification.entity.Severity;
import com.nexora.notification.realtime.SseHub;
import com.nexora.notification.repository.DeliveryAttemptRepository;
import com.nexora.shared.security.Role;
import com.nexora.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Abstract Factory (channel families) and Factory Method (delivery handler creators). */
class NotificationFactoriesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneOffset.UTC);
    private final User user = new User("demo", "demo@test.local", "Demo User", "x", Role.EMPLOYEE, null);
    private final Notification notification = new Notification(1L, NotificationCategory.SYSTEM, Severity.WARNING,
            "Disk almost full", "Only 5% left", "/x", "SYSTEM", clock.instant());

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    private static DeliveryAttemptRepository attempts() {
        DeliveryAttemptRepository repo = mock(DeliveryAttemptRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.lastAttemptNo(any(), any())).thenReturn(0);
        return repo;
    }

    @Test
    void eachFactoryCreatesAConsistentFamily() {
        NotificationChannelFactory inApp = new InAppChannelFactory(new SseHub());
        NotificationChannelFactory email = new EmailChannelFactory(provider(null), TestFixtures.settings());

        assertThat(inApp.createSender()).isInstanceOf(InAppMessageSender.class);
        assertThat(inApp.createFormatter().getClass().getSimpleName()).isEqualTo("InAppMessageFormatter");
        assertThat(email.createFormatter().getClass().getSimpleName()).isEqualTo("EmailMessageFormatter");
        assertThat(email.createSender().getClass().getSimpleName()).isEqualTo("EmailMessageSender");
        assertThat(email.createAvailabilityChecker().getClass().getSimpleName()).isEqualTo("EmailAvailabilityChecker");

        var formatted = email.createFormatter().format(notification, user);
        assertThat(formatted.subject()).isEqualTo("[NEXORA][WARNING] Disk almost full");
        assertThat(formatted.body()).startsWith("Құрметті Demo User");
        assertThat(inApp.createFormatter().format(notification, user).body()).isEqualTo("Only 5% left");
    }

    @Test
    void emailIsSkippedHonestlyWhenSmtpIsNotConfigured() {
        var creator = new EmailDeliveryHandlerCreator(attempts(), clock,
                new EmailChannelFactory(provider(null), TestFixtures.settings()));
        DeliveryAttempt attempt = creator.deliver(notification, user);
        assertThat(attempt.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(attempt.getStatus()).isEqualTo(DeliveryAttempt.Status.SKIPPED);
        assertThat(attempt.getDetail()).contains("SMTP бапталмаған");
        assertThat(creator.retryable()).isTrue();
    }

    @Test
    void emailIsDeliveredOnlyWhenSmtpAcceptsIt() {
        JavaMailSender sender = mock(JavaMailSender.class);
        var creator = new EmailDeliveryHandlerCreator(attempts(), clock,
                new EmailChannelFactory(provider(sender), TestFixtures.settings("minimum-level", true)));
        DeliveryAttempt attempt = creator.deliver(notification, user);
        assertThat(attempt.getStatus()).isEqualTo(DeliveryAttempt.Status.DELIVERED);
        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void inAppCreatorProducesNonRetryableHandler() {
        DeliveryAttemptRepository repo = attempts();
        var creator = new InAppDeliveryHandlerCreator(repo, clock, new InAppChannelFactory(new SseHub()));
        DeliveryAttempt attempt = creator.deliver(notification, user);
        assertThat(attempt.getStatus()).isEqualTo(DeliveryAttempt.Status.DELIVERED);
        assertThat(attempt.getAttemptNo()).isEqualTo(1);
        assertThat(creator.retryable()).isFalse();
        verify(repo).lastAttemptNo(eq(null), eq(NotificationChannel.IN_APP));
    }
}
