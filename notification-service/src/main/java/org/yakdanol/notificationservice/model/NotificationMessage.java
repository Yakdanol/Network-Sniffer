package org.yakdanol.notificationservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сообщение, которым обмениваются микросервисы через Kafka.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotificationMessage {

    /** Имя компьютера-источника. */
    private String internalUserName;

    /** Категория угрозы (фишинг, реклама, ИИ и т.д.). */
    private String category;

    /** IP-адрес или домен-источник угрозы. */
    private String source;

    /** Данные источника угрозы. */
    private String data;

    /** Время обнаружения угрозы. */
    @JsonFormat(pattern = "yyyy:MM:dd HH:mm:ss")
    private LocalDateTime timestamp;

    /** Сообщение с описанием инцидента. */
    private String message;
}
