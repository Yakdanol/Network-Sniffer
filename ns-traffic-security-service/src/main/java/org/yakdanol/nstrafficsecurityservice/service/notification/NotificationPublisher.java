package org.yakdanol.nstrafficsecurityservice.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private static final String TOPIC = "notification-topic";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Формирует {@link NotificationMessage}, сериализует его в JSON
     * и отправляет в Kafka-топик <b>notifications</b>.
     *
     * @param ip обнаруженный IP-адрес
     * @param category категория угрозы (PHISHING, ADVERTISING …)
     * @param internalUserName имя ПК / учётки сотрудника
     */
    public void publish(String ip, String category, String internalUserName) {
        NotificationMessage msg = new NotificationMessage(
                internalUserName,
                category,
                "IP-address",
                ip,
                LocalDateTime.now(),
                String.format("User %s accessed %s resource %s", internalUserName, category.toLowerCase(), ip)
        );

        try {
            String json = objectMapper.writeValueAsString(msg);
            kafkaTemplate.send(TOPIC, json);
            log.debug("Notification sent: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize NotificationMessage {}", msg, e);
        }
    }
}
