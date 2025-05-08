package org.yakdanol.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.yakdanol.notificationservice.service.EmailNotificationSender;
import org.yakdanol.notificationservice.service.RecipientService;
import org.yakdanol.notificationservice.service.TelegramNotificationSender;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final RecipientService recipientService;

    private final EmailNotificationSender emailNotificationSender;

    private final TelegramNotificationSender telegramNotificationSender;

    @Scheduled(cron = "0 0 0 * * ?") // Ежедневно в полночь
    public void dailyUpdate() {
        log.info("Starting daily refresh of recipients.");

        recipientService.refreshRecipients();
        emailNotificationSender.refreshRecipients();
        telegramNotificationSender.refreshRecipients();

        log.info("Daily refresh of recipients completed.");
    }
}
