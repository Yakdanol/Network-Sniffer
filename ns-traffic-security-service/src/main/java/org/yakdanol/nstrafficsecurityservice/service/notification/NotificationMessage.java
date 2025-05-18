package org.yakdanol.nstrafficsecurityservice.service.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotificationMessage {

    /** PC-name / internal user name */
    private String internalUserName;

    /** Категория угрозы */
    private String category;

    /** Тип источника угрозы. По требованию – всегда строка "IP-address" */
    private String source;

    /** Значение источника (сам IP-адрес) */
    private String data;

    /** Время обнаружения */
    @JsonFormat(pattern = "yyyy:MM:dd HH:mm:ss")
    private LocalDateTime timestamp;

    /** Описание инцидента */
    private String message;
}
