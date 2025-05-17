package org.yakdanol.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.yakdanol.notificationservice.model.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yakdanol.notificationservice.service.sender.NotificationSender;
import org.yakdanol.notificationservice.service.sender.SenderFactory;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SenderFactory senderFactory;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.consumer.topics[0]}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String messageJson) {
        try {
            NotificationMessage message = objectMapper.readValue(messageJson, NotificationMessage.class);
            log.info("Received notification message: {}", message);

            List<NotificationSender> senders = senderFactory.getActiveSenders();
            for (NotificationSender sender : senders) {
                sender.send(message);
            }
        } catch (Exception e) {
            log.error("Failed to process notification message.", e);
        }
    }
}
