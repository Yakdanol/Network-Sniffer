package org.yakdanol.nstrafficsecurityservice.service.security;

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
import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatManager;

import java.time.Clock;
import java.time.Instant;
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

    @Autowired
    public TrafficSecurityService(ThreatManager threatManager,
                                  PdfReportService pdfReportService,
                                  MeterRegistry registry) {
        this.threatManager = threatManager;
        this.pdfReportService = pdfReportService;
        this.registry = registry;
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
    public void analyse(PacketConsumer consumer, String userFullName) {

        List<ThreatManager.DetectedThreat> threats = new ArrayList<>();
        Instant started = clock.instant();
        long packetCount = 0;

        try (consumer) {
            List<Packet> batch;
            while (!(batch = consumer.getPackets()).isEmpty()) {
                packetCount += batch.size();
                packetsTotal.increment(batch.size());
                int hits = threatManager.detectThreats(batch, userFullName, threats);
                threatsTotal.increment(hits);
            }
        } catch (IllegalRawDataException | NotOpenException e) {
            logger.error("TrafficSecurityService: Failed to analyse packet for user {}, with exception {} ", userFullName, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        pdfReportService.buildReport(userFullName, started, clock.instant(), packetCount, threats);
    }
}
