package org.yakdanol.notificationservice.service;

import org.yakdanol.notificationservice.model.NotificationMessage;

public interface NotificationSender {
    void send(NotificationMessage message);
}
