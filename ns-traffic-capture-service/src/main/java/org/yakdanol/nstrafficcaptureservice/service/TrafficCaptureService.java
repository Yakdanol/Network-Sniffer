package org.yakdanol.nstrafficcaptureservice.service;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.metrics.TrafficCaptureMetrics;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.util.concurrent.*;

@Slf4j
@Service
public class TrafficCaptureService {
    private final PcapHandle handle;
    private final CapturedPacketRepository repository;
    private final PacketToJsonConverter converter;
    private final FileRotationService fileRotationService;
    private final TrafficCaptureMetrics metrics;

    private final BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>(100);
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(4); // Пул из 4 потоков

    @Value("${traffic-capture.filter}")
    private String filter;

    public TrafficCaptureService(PcapNetworkInterface networkInterface,
                                 CapturedPacketRepository repository,
                                 PacketToJsonConverter converter,
                                 FileRotationService fileRotationService,
                                 MeterRegistry meterRegistry) throws PcapNativeException, NotOpenException {
        this.handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
        this.handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
        this.repository = repository;
        this.converter = converter;
        this.fileRotationService = fileRotationService;
        this.metrics = new TrafficCaptureMetrics(meterRegistry);
    }

    @PostConstruct
    public void startCapture() {
        captureExecutor.submit(() -> {
            try {
                PacketListener listener = this::enqueuePacket;
                handle.loop(-1, listener);
            } catch (InterruptedException e) {
                log.warn("Packet capture interrupted", e);
                Thread.currentThread().interrupt();
            } catch (PcapNativeException | NotOpenException e) {
                log.error("Error in packet capture", e);
            }
        });

        // Запуск задач обработки пакетов
        for (int i = 0; i < 4; i++) { // 4 потока обработки
            processingExecutor.submit(this::consumePackets);
        }

        // Запуск сервиса ротации файлов
        fileRotationService.start();
    }

    private void enqueuePacket(Packet packet) {
        try {
            packetQueue.put(packet);
            metrics.incrementCapturedPackets();
        } catch (InterruptedException e) {
            log.error("Interrupted while enqueuing packet", e);
            Thread.currentThread().interrupt();
        }
    }

    private void consumePackets() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Packet packet = packetQueue.take();
                CapturedPacket capturedPacket = converter.convert(packet);
                repository.save(capturedPacket);
            } catch (InterruptedException e) {
                log.warn("Packet processing thread interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                metrics.incrementProcessingErrors();
                log.error("Error processing packet from queue", e);
            }
        }
    }

    @PreDestroy
    public void stopCapture() {
        try {
            handle.breakLoop();
            handle.close();
        } catch (NotOpenException e) {
            log.error("Error closing pcap handle", e);
        }
        captureExecutor.shutdownNow();
        processingExecutor.shutdownNow();
        try {
            if (!captureExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("CaptureExecutor did not terminate in the specified time.");
            }
            if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("ProcessingExecutor did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during shutdown", e);
            Thread.currentThread().interrupt();
        }
        fileRotationService.stop();
    }
}
