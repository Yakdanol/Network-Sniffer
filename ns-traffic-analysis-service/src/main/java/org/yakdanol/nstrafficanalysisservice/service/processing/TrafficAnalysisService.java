package org.yakdanol.nstrafficanalysisservice.service.processing;

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
import org.yakdanol.nstrafficanalysisservice.service.analysis.AnalysisManager;
import org.yakdanol.nstrafficanalysisservice.service.consumer.PacketConsumer;
import org.yakdanol.nstrafficanalysisservice.service.domain.DomainHit;
import org.yakdanol.nstrafficanalysisservice.service.domain.PacketProcessor;
import org.yakdanol.nstrafficanalysisservice.service.report.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrafficAnalysisService {
    private final static Logger logger = LoggerFactory.getLogger(TrafficAnalysisService.class);

    private final AnalysisManager analysisManager;
    private final PdfReportService pdfReportService;
    private final MeterRegistry registry;
    private final Clock clock;
    private final Counter packetsTotal;
    private final Counter threatsTotal;
    private final AnalysisReportSummaryRepository analysisReportSummaryRepository;

    @Autowired
    public TrafficAnalysisService(AnalysisManager analysisManager,
                                  PdfReportService pdfReportService,
                                  MeterRegistry registry,
                                  AnalysisReportSummaryRepository analysisReportSummaryRepository) {
        this.analysisManager = analysisManager;
        this.pdfReportService = pdfReportService;
        this.registry = registry;
        this.analysisReportSummaryRepository = analysisReportSummaryRepository;
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
        PacketProcessor processor = new PacketProcessor();
        List<DomainHit> allDomains = new ArrayList<>();
        List<AnalysisManager.DetectedThreat> threats = new ArrayList<>();
        LocalDateTime started  = LocalDateTime.now(clock);
        long packetCount = 0;

        try (consumer) {
            List<Packet> batch;
            while (!(batch = consumer.getPackets()).isEmpty()) {
                for (Packet packet : batch) {
                    List<DomainHit> hits = processor.accept(packet);
                    allDomains.addAll(hits);
                    int fired = analysisManager.detectThreats(hits, internalUserName, threats);
                    threatsTotal.increment(fired);
                }
                packetCount += batch.size();
            }
        } catch (IllegalRawDataException | NotOpenException e) {
            logger.error("TrafficAnalysisService: Failed to analyse packet for user {}, with exception {} ", internalUserName, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        LocalDateTime finished = LocalDateTime.now(clock);
        Duration dur = Duration.between(started, finished);
        String durationStr = String.format("%02d:%02d:%02d", dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart()
        );
        AnalysisReportSummary summary = AnalysisReportSummary.builder()
                .fullName(fullName)
                .startedAt(started)
                .duration(durationStr)
                .packetsProcessed(packetCount)
                .threatsFound(threats.size())
                .build();
        analysisReportSummaryRepository.save(summary);
        pdfReportService.buildReport(fullName, started, finished, packetCount, threats);
    }
}
