package org.yakdanol.nstrafficsecurityservice.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Отправляет в сервис нотификаций строку формата:
     * {@code 202.54.10.1|PHISHING|User «Иванов И.И.» обратился к фишинговому ресурсу}.
     */
    public void publish(String ip, String category, String internalUserName) {
        kafkaTemplate.send("notifications", ip + "|" + category + "|User " + internalUserName + " accessed malicious resource");
    }
}
