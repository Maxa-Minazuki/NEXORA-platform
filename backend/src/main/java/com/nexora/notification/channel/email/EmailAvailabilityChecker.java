package com.nexora.notification.channel.email;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.ChannelAvailabilityChecker;
import com.nexora.shared.config.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailAvailabilityChecker implements ChannelAvailabilityChecker {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final PlatformSettings settings;

    EmailAvailabilityChecker(ObjectProvider<JavaMailSender> mailSender, PlatformSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Override
    public Availability check(User recipient) {
        if (!settings.emailEnabled() || mailSender.getIfAvailable() == null) {
            return Availability.no("SMTP бапталмаған (демо режимі)");
        }
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return Availability.no("Алушының e-mail мекенжайы жоқ");
        }
        return Availability.yes();
    }
}
