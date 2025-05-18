package org.yakdanol.nstrafficsecurityservice.service.report;

import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatManager;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    void buildReport(
            String user,
            LocalDateTime from,
            LocalDateTime to,
            long packets,
            List<ThreatManager.DetectedThreat> threats);
}
