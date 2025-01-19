package org.yakdanol.nstrafficcaptureservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.metrics.TrafficCaptureMetrics;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.util.concurrent.*;

@Slf4j
@Service
public class TrafficCaptureService {

    private final TrafficCaptureConfig config;
    private static final Logger logger = LoggerFactory.getLogger(TrafficCaptureService.class);
    private final PcapHandle handle;
    private final CapturedPacketRepository repository;
    private final PacketToJsonConverter converter;
    private final FileRotationService fileRotationService;
    private final TrafficCaptureMetrics metrics;
    private final BlockingQueue<Packet> packetQueue;
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService processingExecutor;

    @Autowired
    public TrafficCaptureService(TrafficCaptureConfig config,
                                 PcapNetworkInterface networkInterface,
                                 CapturedPacketRepository repository,
                                 PacketToJsonConverter converter,
                                 FileRotationService fileRotationService,
                                 MeterRegistry meterRegistry) throws PcapNativeException, NotOpenException {
        this.config = config;
        this.handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
//        this.handle.setFilter(trafficCaptureConfig.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);
        this.repository = repository;
        this.converter = converter;
        this.fileRotationService = fileRotationService;
        this.metrics = new TrafficCaptureMetrics(meterRegistry);
        this.processingExecutor = Executors.newFixedThreadPool(config.getProcessingPoolSize());
        this.packetQueue = new LinkedBlockingQueue<>(config.getQueueSize());
    }

    @PostConstruct
    public void startCapture() {
        captureExecutor.submit(() -> {
            try {
                PacketListener listener = this::enqueuePacket;
                handle.loop(-1, listener);
            } catch (InterruptedException e) {
                logger.warn("Packet capture interrupted", e);
                Thread.currentThread().interrupt();
            } catch (PcapNativeException | NotOpenException e) {
                logger.error("Error in packet capture", e);
            }
        });

        // Запуск задач обработки пакетов
        for (int i = 0; i < config.getProcessingPoolSize(); i++) {
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
            logger.error("Interrupted while enqueuing packet", e);
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
                logger.warn("Packet processing thread interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                metrics.incrementProcessingErrors();
                logger.error("Error processing packet from queue", e);
            }
        }
    }

    @PreDestroy
    public void stopCapture() {
        try {
            handle.breakLoop();
            handle.close();
        } catch (NotOpenException e) {
            logger.error("Error closing pcap handle", e);
        }
        captureExecutor.shutdownNow();
        processingExecutor.shutdownNow();
        try {
            if (!captureExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("CaptureExecutor did not terminate in the specified time.");
            }
            if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("ProcessingExecutor did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during shutdown", e);
            Thread.currentThread().interrupt();
        }
        fileRotationService.stop();
    }
}
