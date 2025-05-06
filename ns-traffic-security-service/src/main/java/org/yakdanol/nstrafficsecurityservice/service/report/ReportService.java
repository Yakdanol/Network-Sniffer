package org.yakdanol.nstrafficsecurityservice.service.report;

import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatManager;

import java.time.Instant;
import java.util.List;

public interface ReportService {
    void buildReport(String user, Instant from, Instant to,
                            long packets, List<ThreatManager.DetectedThreat> threats);
}
