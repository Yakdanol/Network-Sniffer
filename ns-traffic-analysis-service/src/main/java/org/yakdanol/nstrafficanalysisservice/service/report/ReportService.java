package org.yakdanol.nstrafficanalysisservice.service.report;

import org.yakdanol.nstrafficanalysisservice.service.analysis.AnalysisManager;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    void buildReport(
            String user,
            LocalDateTime from,
            LocalDateTime to,
            long packets,
            List<AnalysisManager.DetectedThreat> threats);
}
