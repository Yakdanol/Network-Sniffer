package org.yakdanol.notificationservice.service.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yakdanol.notificationservice.config.MailConfig;
import org.yakdanol.notificationservice.config.TelegramConfig;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SenderFactory {

    private final EmailNotificationSender emailSender;
    private final TelegramNotificationSender telegramSender;
    private final MailConfig mailConfig;
    private final TelegramConfig telegramConfig;

    public List<NotificationSender> getActiveSenders() {
        List<NotificationSender> senders = new ArrayList<>();
        if (mailConfig.isEmailEnabled()) {
            senders.add(emailSender);
        }
        if (telegramConfig.isTelegramEnabled()) {
            senders.add(telegramSender);
        }
        return senders;
    }
}
