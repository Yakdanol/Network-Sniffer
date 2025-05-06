package org.yakdanol.nstrafficsecurityservice.users.storage;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String internalUserName; // Внутреннее имя сотрудника в системе и имя личного ПК

    private String fullName; // ФИО сотрудника

    private String position; // Должность сотрудника

    private String phoneNumber; // Номер телефона сотрудника

    private String email; // Почта сотрудника

    private String telegramAccount; // Имя Телеграмм-аккаунта сотрудника

    private String kafkaTopicName; // Имя топика Kafka, куда сотрудник отправляет сетевые пакеты для анализа
}
