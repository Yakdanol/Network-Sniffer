package org.yakdanol.notificationservice.service.sender;

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
import org.yakdanol.notificationservice.service.RecipientService;
import org.yakdanol.notificationservice.users.NotificationUsers;
import org.yakdanol.notificationservice.users.NotificationUsersRepository;
import org.yakdanol.notificationservice.utils.MessageFormatter;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final RecipientService recipientService;
    private final MailConfig mailConfig;
    private final NotificationUsersRepository notificationUsersRepository;

    @Override
    @Async
    @Retryable(
            value = { Exception.class },
            backoff = @Backoff(delay = 5000)
    )
    public void send(NotificationMessage message) {
        List<String> emailRecipients = recipientService.getEmailRecipients(message.getInternalUserName());
        if (emailRecipients.isEmpty()) {
            log.warn("No email recipients found.");
            return;
        }

        if (!mailConfig.isEmailEnabled()) {
            log.debug("Email notifications are disabled in configuration.");
            return;
        }

        NotificationUsers user = notificationUsersRepository.findByInternalUserName(message.getInternalUserName());
        MessageFormatter.EmailContent emailContent = MessageFormatter.buildEmail(message, user);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailConfig.getSmtpUsername());
        mailMessage.setTo(emailRecipients.toArray(String[]::new));
        mailMessage.setSubject(emailContent.subject());
        mailMessage.setText(emailContent.body());

        try {
            mailSender.send(mailMessage);
        } catch (Exception e) {
            log.error("Failed to send email notifications. Attempting retry.", e);
            throw e;
        }
        log.info("Email sent: category={} user={} recipients={}",
                message.getCategory(), message.getInternalUserName(), emailRecipients.size());
    }
}
