//package org.yakdanol.nstrafficanalysisservice.model.reports;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@AllArgsConstructor
//@RequiredArgsConstructor
//@Entity
//@Table(name = "reports")
//public class Reports {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    /** ФИО сотрудника. */
//    @Column(nullable = false)
//    private String fullName;
//
//    /** Внутреннее имя компьютера сотрудника в системе. */
//    @Column(nullable = false)
//    private String internalUserName;
//
//    /** Должность сотрудника. */
//    @Column(nullable = false)
//    private String position;
//
//    /**
//     * Источник данных или (по описанию)
//     * общее число обработанных пакетов.
//     */
//    @Column(nullable = false)
//    private DataSource dataSource;
//
//    /** Количество небезопасных/подозрительных пакетов. */
//    private Long totalNumberPackages;
//
//    /** Дата и время начала анализа. */
//    private LocalDateTime dateAnalysis;
//
//    /**
//     * Продолжительность анализа.
//     */
//    private Duration durationAnalysis;
//}
