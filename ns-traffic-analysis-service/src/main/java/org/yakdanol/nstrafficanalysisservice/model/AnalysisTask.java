//package org.yakdanol.nstrafficanalysisservice.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.LocalDateTime;
//
///*
// * Внутренняя модель сущности AnalysisTask, хранимая в PostgreSQL.
// * Содержит служебные поля, статус и информацию о ходе анализа.
// */
//
//@Entity
//@Table(name = "analysis_tasks")
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class AnalysisTask {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private String id;
//    private long taskId;
//    private String userName;
//
//    private String typeTask;          // "FILE" / "KAFKA"
//    private String topicName;         // если KAFKA
//    private String fileName;          // если FILE (для единственного)
//    private String status;            // QUEUED / RUNNING / DONE / ERROR
//    private long processedPackets;    // сколько обработано
//    private long totalPackets;        // общее кол-во обработанных пакетов
//    private double percentProcessed;           // (processed / totalPackets)*100
//    private String errorMessage;      // Сообщение об ошибке при ERROR
//    private LocalDateTime startTime;  // Время начало анализа
//    private LocalDateTime endTime;    // Время конца анализа
//    private String reportPath;        // Путь к отчету
//    private String analysisResult;    // Результат анализа
//}
