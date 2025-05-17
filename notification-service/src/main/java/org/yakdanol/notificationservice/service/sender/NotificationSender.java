package org.yakdanol.notificationservice.service.sender;

import org.yakdanol.notificationservice.model.NotificationMessage;

public interface NotificationSender {
    void send(NotificationMessage message);
}
