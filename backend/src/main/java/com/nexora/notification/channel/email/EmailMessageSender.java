package com.nexora.notification.channel.email;

import com.nexora.auth.entity.User;
import com.nexora.notification.channel.FormattedMessage;
import com.nexora.notification.channel.MessageSender;
import com.nexora.notification.channel.SendResult;
import com.nexora.notification.entity.Notification;
import com.nexora.shared.config.PlatformSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Sends through SMTP. Never reports success unless the SMTP server accepted the message. */
public class EmailMessageSender implements MessageSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final PlatformSettings settings;

    EmailMessageSender(ObjectProvider<JavaMailSender> mailSender, PlatformSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    @Override
    public SendResult send(FormattedMessage message, Notification n, User recipient) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !settings.emailEnabled()) {
            return SendResult.skipped("SMTP бапталмаған (демо режимі) - хат жіберілмеді");
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(settings.emailFrom());
            mail.setTo(recipient.getEmail());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            sender.send(mail);
            return SendResult.delivered("SMTP сервері " + recipient.getEmail() + " үшін қабылдады");
        } catch (MailException ex) {
            return SendResult.failed("SMTP қатесі: " + ex.getMessage());
        }
    }
}
