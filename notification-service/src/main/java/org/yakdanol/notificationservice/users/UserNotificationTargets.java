package org.yakdanol.notificationservice.users;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * Таблица маршрутизации: «если у этого сотрудника инцидент – уведомить …».
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_notification_targets")
public class UserNotificationTargets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Внутреннее имя ПК сотрудника. */
    @Column(nullable = false, unique = true)
    private String monitoredInternalUserName;

    /** Список email-адресов, которым необходимо отправить уведомление. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_targets_emails",
            joinColumns = @JoinColumn(name = "target_id"))
    @Column(name = "email")
    private Set<String> emails;

    /** Список chat-id в Telegram, которым необходимо отправить уведомление. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_targets_telegrams",
            joinColumns = @JoinColumn(name = "target_id"))
    @Column(name = "chat_id")
    private Set<String> telegramChatIds;
}
