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

    /**
     * Внутреннее имя компьютера сотрудника в системе
     */
    @Column(nullable = false)
    private String internalUserName;

    /**
     * ФИО сотрудника
     */
    @Column(nullable = false)
    private String fullName;

    /**
     * Должность сотрудника
     */
    @Column(nullable = false)
    private String position;

    /**
     * Имя топика Kafka, куда сотрудник отправляет сетевые пакеты для анализа
     */
    @Column(nullable = false)
    private String kafkaTopicName;
}
