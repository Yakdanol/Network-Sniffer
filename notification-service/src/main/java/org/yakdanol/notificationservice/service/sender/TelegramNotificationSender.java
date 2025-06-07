package org.yakdanol.notificationservice.service.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.yakdanol.notificationservice.config.TelegramConfig;
import org.yakdanol.notificationservice.model.NotificationMessage;
import org.yakdanol.notificationservice.service.RecipientService;
import org.yakdanol.notificationservice.users.NotificationUsers;
import org.yakdanol.notificationservice.users.NotificationUsersRepository;
import org.yakdanol.notificationservice.utils.MessageFormatter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationSender implements NotificationSender {

    private final RecipientService recipientService;
    private final TelegramConfig telegramConfig;
    private final NotificationUsersRepository notificationUsersRepository;

    @Qualifier("telegramRestTemplate")
    private final RestTemplate telegramRestTemplate;

    @Override
    @Async
    @Retryable(
            value = { Exception.class },
            maxAttempts = 2,
            backoff = @Backoff(delay = 5000)
    )
    public void send(NotificationMessage message) {
        List<String> telegramRecipients = recipientService.getTelegramRecipients(message.getInternalUserName());
        if (telegramRecipients.isEmpty()) {
            log.warn("No Telegram recipients found for {}.", message.getInternalUserName());
            return;
        }

        if (!telegramConfig.isTelegramEnabled()) {
            log.debug("Telegram notifications are disabled in configuration.");
            return;
        }

        NotificationUsers user = notificationUsersRepository.findByInternalUserName(message.getInternalUserName());
        String text = MessageFormatter.buildTelegramText(message, user);
        String baseUrl = "https://api.telegram.org/bot%s/sendMessage".formatted(telegramConfig.getTelegramBotToken());

        for (String chatId : telegramRecipients) {
            try {
                var uri = UriComponentsBuilder.fromUriString(baseUrl)
                        .queryParam("chat_id", chatId)
                        .queryParam("text", text)
                        .queryParam("parse_mode", "MarkdownV2")
                        .encode(StandardCharsets.UTF_8)
                        .build()
                        .toUri();
                telegramRestTemplate.getForObject(uri, String.class);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification to chatId: {}. Attempting retry.", chatId, e);
                throw e;
            }
        }
        log.info("Telegram sent: category={} user={} recipients={}",
                message.getCategory(), message.getInternalUserName(), telegramRecipients.size());
    }
}
