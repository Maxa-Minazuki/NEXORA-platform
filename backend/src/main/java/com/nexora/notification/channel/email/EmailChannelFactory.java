package com.nexora.notification.channel.email;

import com.nexora.notification.channel.ChannelAvailabilityChecker;
import com.nexora.notification.channel.MessageFormatter;
import com.nexora.notification.channel.MessageSender;
import com.nexora.notification.channel.NotificationChannelFactory;
import com.nexora.notification.entity.NotificationChannel;
import com.nexora.shared.config.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Concrete factory: the e-mail family (SMTP via Spring Mail, only when configured). */
@Component
public class EmailChannelFactory implements NotificationChannelFactory {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final PlatformSettings settings;

    public EmailChannelFactory(ObjectProvider<JavaMailSender> mailSender, PlatformSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }
    @Override public MessageFormatter createFormatter() { return new EmailMessageFormatter(settings.platformName()); }
    @Override public MessageSender createSender() { return new EmailMessageSender(mailSender, settings); }
    @Override public ChannelAvailabilityChecker createAvailabilityChecker() {
        return new EmailAvailabilityChecker(mailSender, settings);
    }
}
