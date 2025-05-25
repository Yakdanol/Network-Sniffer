package org.yakdanol.nstrafficsecurityservice.service.processing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficsecurityservice.service.consumer.PacketConsumer;
import org.yakdanol.nstrafficsecurityservice.service.report.PdfReportService;
import org.yakdanol.nstrafficsecurityservice.service.report.SecurityReportSummary;
import org.yakdanol.nstrafficsecurityservice.service.report.SecurityReportSummaryRepository;
import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatManager;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrafficSecurityService {
    private final static Logger logger = LoggerFactory.getLogger(TrafficSecurityService.class);

    private final ThreatManager threatManager;
    private final PdfReportService pdfReportService;
    private final MeterRegistry registry;
    private final Clock clock;
    private final Counter packetsTotal;
    private final Counter threatsTotal;
    private final SecurityReportSummaryRepository securityReportSummaryRepository;

    @Autowired
    public TrafficSecurityService(ThreatManager threatManager,
                                  PdfReportService pdfReportService,
                                  MeterRegistry registry, SecurityReportSummaryRepository securityReportSummaryRepository) {
        this.threatManager = threatManager;
        this.pdfReportService = pdfReportService;
        this.registry = registry;
        this.securityReportSummaryRepository = securityReportSummaryRepository;
        this.clock = Clock.systemUTC();

        this.packetsTotal = Counter.builder("packets_total")
                .description("All processed packets")
                .register(registry);

        this.threatsTotal = Counter.builder("threats_detected_total")
                .description("Total found threats")
                .register(registry);
    }

    /**
     * Запускает анализ и возвращает список угроз — понадобится для PDF‑отчёта.
     */
    public void analyse(PacketConsumer consumer, String internalUserName, String fullName) {
        List<ThreatManager.DetectedThreat> threats = new ArrayList<>();
        LocalDateTime started  = LocalDateTime.now(clock);
        long packetCount = 0;

        try (consumer) {
            List<Packet> batch;
            while (!(batch = consumer.getPackets()).isEmpty()) {
                packetCount += batch.size();
                packetsTotal.increment(batch.size());
                int hits = threatManager.detectThreats(batch, internalUserName, threats);
                threatsTotal.increment(hits);
            }
        } catch (IllegalRawDataException | NotOpenException e) {
            logger.error("TrafficSecurityService: Failed to analyse packet for user {}, with exception {} ", internalUserName, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        LocalDateTime finished = LocalDateTime.now(clock);
        Duration dur = Duration.between(started, finished);
        String durationStr = String.format("%02d:%02d:%02d", dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart()
        );
        SecurityReportSummary summary = SecurityReportSummary.builder()
                .fullName(fullName)
                .startedAt(started)
                .duration(durationStr)
                .packetsProcessed(packetCount)
                .threatsFound(threats.size())
                .build();
        securityReportSummaryRepository.save(summary);
        pdfReportService.buildReport(fullName, started, finished, packetCount, threats);
    }
}
