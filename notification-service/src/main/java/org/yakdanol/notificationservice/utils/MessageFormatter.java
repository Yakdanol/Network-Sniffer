package org.yakdanol.notificationservice.utils;

import lombok.experimental.UtilityClass;
import org.yakdanol.notificationservice.model.NotificationMessage;
import org.yakdanol.notificationservice.users.NotificationUsers;

import java.time.format.DateTimeFormatter;

/**
 * Статические методы-шаблоны, формирующие текст уведомлений.
 */
@UtilityClass
public class MessageFormatter {

    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Текст для Telegram.
     */
    public String buildTelegramText(NotificationMessage message, NotificationUsers user) {
        return """
                *Обнаружено небезопасное подключение*
                Детали инцидента:
                ──────────────────────────
                • *Тип угрозы*: %s
                • *Источник*: %s
                • *Данные источника*: %s
                • *Время*: %s
                • *Пользователь*: %s
                • *Должность*: %s
                """.formatted(
                escape(message.getCategory()),
                escape(message.getSource()),
                escape(message.getData()),
                escape(message.getTimestamp().format(DATE_TIME_FORMATTER)),
                escape(user.getFullName()),
                escape(user.getPosition())
        );
    }

    /**
     * Тема и текст для email.
     */
    public EmailContent buildEmail(NotificationMessage message, NotificationUsers user) {
        String subject = "Уведомление о нарушении безопасности: %s - %s".formatted(message.getCategory(), user.getFullName());

        String body = """
                Это автоматическое уведомление о том, что зафиксировано подозрительное подключение.

                Детали инцидента:
                ────────────────────────────
                • Тип угрозы: %s
                • Источник: %s
                • Данные источника: %s
                • Время: %s
                • Пользователь: %s
                • Должность: %s
                ────────────────────────────

                Рекомендуемые действия:
                1. Немедленно разорвать соединение.
                2. Проверить активные сессии.
                3. Сообщить в отдел ИБ (security-team@example.com).

                Спасибо за внимание к безопасности!
                """.formatted(
                message.getCategory(),
                message.getSource(),
                message.getData(),
                message.getTimestamp().format(DATE_TIME_FORMATTER),
                user.getFullName(),
                user.getPosition()
        );

        return new EmailContent(subject, body);
    }

    // --------------------------- helpers -----------------------------

    private String escape(String s) {
        // экранирование символов MarkdownV2
        if (s == null) return "";
        return s.replaceAll("([_\\*\\[\\]\\(\\)~`>#\\+\\-=\\|\\{\\}\\.\\!])", "\\\\$1");
    }

    public record EmailContent(String subject, String body) {}
}
