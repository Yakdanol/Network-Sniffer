package org.yakdanol.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yakdanol.notificationservice.config.TelegramConfig;
import org.yakdanol.notificationservice.model.NotificationMessage;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationSender implements NotificationSender {

    private final RecipientService recipientService;
    private final TelegramConfig telegramConfig;

    @Qualifier(value = "telegramRestTemplate")
    private final RestTemplate telegramRestTemplate;
    private List<String> telegramRecipientsCache;

    @PostConstruct
    public void init() {
        refreshRecipients();
    }

    public void refreshRecipients() {
        telegramRecipientsCache = recipientService.getTelegramRecipients();
        log.info("TelegramNotificationSender recipients updated: {} recipients loaded.", telegramRecipientsCache.size());
    }

    @Override
    @Async
    @Retryable(
            value = { Exception.class },
            maxAttempts = 2,
            backoff = @Backoff(delay = 5000)
    )
    public void send(NotificationMessage message) {
        if (telegramRecipientsCache.isEmpty()) {
            log.warn("No Telegram recipients found.");
            return;
        }

        if (!telegramConfig.isTelegramEnabled()) {
            log.debug("Telegram notifications are disabled in configuration.");
            return;
        }

        String botToken = telegramConfig.getTelegramBotToken();
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

//        String text = message.getMessage() + "\nМедикамент: " + message.getMedicationName();
        String text = "Обнаружено небезопасное подключение \n" +
                "Детали инцидента: \n" +
                "──────────────────────────\n" +
                "• Тип угрозы: Фишинг \n" +
                "• IP-адрес источника: 144.172.64.51 \n" +
                "• Время обнаружения: 2025-05-08 17:29 \n" +
                "• Пользователь: Иванов Иван Иванович \n" +
                "• Должность: Java Разработчик";
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);

        for (String chatId : telegramRecipientsCache) {
            try {
                URI uri = URI.create(apiUrl + "?chat_id=" + chatId + "&text=" + encodedText);
                telegramRestTemplate.getForEntity(uri, String.class);
                log.info("Telegram notification sent to chatId: {}", chatId);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification to chatId: {}. Attempting retry.", chatId, e);
                throw e; // Чтобы сработал Retry
            }
        }
    }
}
