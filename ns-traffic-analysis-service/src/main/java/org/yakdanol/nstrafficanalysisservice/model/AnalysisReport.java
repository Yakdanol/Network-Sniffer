package org.yakdanol.nstrafficanalysisservice.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport {
    private LocalDateTime analysisDate;
    private String user;
    private String totalTimeProcessed;
    private long totalProcessed;          // Общее кол-во обработанных пакетов
    private Map<String, Long> categoryCount; // Категории подозрительности
    private String conclusion;            // Вывод после анализа
}
