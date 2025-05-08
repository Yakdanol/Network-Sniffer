package org.yakdanol.notificationservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationMessage {

    private String type; // Тип уведомления: "Заканчивается срок годности", "Заканчивается лекарство"

    private String message;

    private String department; // Отделение

    private String medicationId;

    private String medicationName;

    private int quantity; // Количество
}
