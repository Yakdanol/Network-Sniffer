package org.yakdanol.nstrafficanalysisservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.model.AnalysisReport;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class ReportGeneratorService {

    // Временная заглушка
    public AnalysisReport generateReport(long totalProcessed, String user) {
        AnalysisReport report = AnalysisReport.builder()
                .analysisDate(LocalDateTime.now())
                .user(user)
                .totalProcessed(totalProcessed)
                .categoryCount(Map.of("UNKNOWN", totalProcessed)) // временная заглушка
                .conclusion("Analysis done. No suspicious packets found.")
                .build();
        log.info("Report generated with totalProcessed={}", totalProcessed);
        return report;
    }
}
