package org.yakdanol.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.yakdanol.notificationservice.config.MailConfig;
import org.yakdanol.notificationservice.model.NotificationMessage;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final RecipientService recipientService;
    private final MailConfig mailConfig;
    private List<String> emailRecipientsCache;

    @PostConstruct
    public void init() {
        refreshRecipients();
    }

    public void refreshRecipients() {
        emailRecipientsCache = recipientService.getEmailRecipients();
        log.info("EmailNotificationSender recipients updated: {} recipients loaded.", emailRecipientsCache.size());
    }

    @Override
    @Async
    @Retryable(
            value = { Exception.class },
            backoff = @Backoff(delay = 5000)
    )
    public void send(NotificationMessage message) {
        if (emailRecipientsCache.isEmpty()) {
            log.warn("No email recipients found.");
            return;
        }

        if (!mailConfig.isEmailEnabled()) {
            log.debug("Email notifications are disabled in configuration.");
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailConfig.getSmtpUsername());
        mailMessage.setTo(emailRecipientsCache.toArray(new String[0]));
        mailMessage.setSubject("Уведомление о нарушении безопасности: " + message.getType() + " - " + message.getMedicationName());
        mailMessage.setText("Это автоматическое уведомление от сервиса безопасности" + message.getMessage());

        try {
            mailSender.send(mailMessage);
            log.info("Email notification sent to {} recipients.", emailRecipientsCache.size());
        } catch (Exception e) {
            log.error("Failed to send email notifications. Attempting retry.", e);
            throw e; // Чтобы сработал Retry
        }
    }
}
