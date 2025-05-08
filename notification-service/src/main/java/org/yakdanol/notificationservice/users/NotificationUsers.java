package org.yakdanol.notificationservice.users;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_users")
public class NotificationUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Полное имя сотрудника (ФИО). */
    @Column(nullable = false)
    private String fullName;

    /** Внутреннее имя компьютера. */
    @Column(nullable = false, unique = true)
    private String internalUserName;

    /** Должность сотрудника. */
    @Column(nullable = false)
    private String position;

    /** Номер телефона. */
    private String phoneNumber;

    /** Email для отправки уведомлений. */
    private String email;

    /** Телеграм-аккаунт для отправки уведомлений. */
    private String telegramAccount;
}
