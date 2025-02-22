package org.yakdanol.nstrafficcaptureservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.metrics.TrafficCaptureMetrics;

import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class TrafficCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(TrafficCaptureService.class);

    private final TrafficCaptureConfig config;
    private final TrafficCaptureMetrics metrics;
    private final KafkaPacketSender kafkaSender;
    private final LocalFilePacketSender localFileSender;

    private volatile PacketSender activeSender; // текущая реализация отправки (Kafka / Local)
    private boolean doubleWrite;

    private final ExecutorService captureExecutor;
    private final ExecutorService processingExecutor;
    private final BlockingQueue<Packet> packetQueue;
    private final PcapHandle handle;
    private final FileRotationService fileRotationService;

    @Autowired
    public TrafficCaptureService(TrafficCaptureConfig config,
                                 PcapNetworkInterface networkInterface,
                                 FileRotationService fileRotationService,
                                 MeterRegistry meterRegistry,
                                 @Qualifier("kafkaPacketSender") KafkaPacketSender kafkaSender,
                                 @Qualifier("localFilePacketSender") LocalFilePacketSender localFileSender) throws PcapNativeException, NotOpenException {
        this.config = config;
        this.fileRotationService = fileRotationService;
        this.metrics = new TrafficCaptureMetrics(meterRegistry);

        this.kafkaSender = kafkaSender;
        this.localFileSender = localFileSender;

        this.handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
        this.handle.setFilter(config.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

        this.captureExecutor = Executors.newFixedThreadPool(config.getCapturingPoolSize());
        this.processingExecutor = Executors.newFixedThreadPool(config.getProcessingPoolSize());
        this.packetQueue = new LinkedBlockingQueue<>(config.getQueueSize());

        logger.info("TrafficCaptureService constructor: pcap handle opened, filter={}, mode={}",
                config.getFilter(), config.getMode());
    }

    @PostConstruct
    public void init() {
        // Определяем тип работы сервиса (Local / Remote)
        String mode = config.getMode();
        logger.info("TrafficCaptureService starting in mode: {}", mode);

        switch (mode.toLowerCase()) {
            case "remote" -> {
                if (kafkaSender.checkAvailable()) {
                    activeSender = kafkaSender;
                    doubleWrite = false;
                    logger.info("Kafka is available. Using remote mode only (Kafka).");
                } else {
                    // переключаемся в local
                    activeSender = localFileSender;
                    doubleWrite = false;
                    logger.warn("Kafka is NOT available. Fallback to local mode.");
                }
            }
            case "both" -> {
                if (kafkaSender.checkAvailable()) {
                    // Если Kafka доступна, используем double-write
                    activeSender = kafkaSender;
                    doubleWrite = true;
                    logger.info("Kafka is available. Using double-write (both) mode.");
                } else {
                    // переключаемся в local
                    activeSender = localFileSender;
                    doubleWrite = false;
                    logger.warn("Kafka is NOT available. Fallback to local mode (no double-write).");
                }
            }
            case "local" -> {
                activeSender = localFileSender;
                doubleWrite = false;
                logger.info("Local mode enabled. Will only write to local files.");
            }
            default -> {
                logger.error("Unknown traffic-capture.mode: {}. Fallback to local.", mode);
                activeSender = localFileSender;
                doubleWrite = false;
            }
        }

        try {
            startCapture();
        } catch (Exception e) {
            logger.error("Failed to init PCAP handle: {}", e.getMessage(), e);
            // Если не удалось открыть сетевой интерфейс - fallback не спасёт, просто завершаем
            shutdownExecutors();
        }
    }

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
                sendPacketWithFallback(packet);
            } catch (InterruptedException e) {
                logger.warn("Packet processing thread interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                metrics.incrementProcessingErrors();
                logger.error("Error processing packet from queue", e);
            }
        }
    }

    private void sendPacketWithFallback(Packet packet) {
        if (doubleWrite) {
            try {
                kafkaSender.sendPacket(packet);
            } catch (Exception ex) {
                logger.error("Kafka send failed in 'both' mode. Fallback partial: we still have local file. Error={}", ex.getMessage());
                doubleWrite = false;
                activeSender = localFileSender;
                localFileSender.sendPacket(packet);
                kafkaSender.closeProducer(); // Закрываем Producer Kafka
                return;
            }
            // Если Kafka ок, то параллельно пишем в локальный файл
            localFileSender.sendPacket(packet);
        } else {
            if (activeSender == kafkaSender) {
                try {
                    kafkaSender.sendPacket(packet);
                } catch (Exception ex) {
                    logger.error("Critical error sending to Kafka. Fallback to local mode. Err={}", ex.getMessage());
                    activeSender = localFileSender;
                    localFileSender.sendPacket(packet);
                    kafkaSender.closeProducer();
                }
            } else {
                localFileSender.sendPacket(packet);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down TrafficCaptureService gracefully...");
        try {
            if (handle != null && handle.isOpen()) {
                handle.breakLoop();
                handle.close();
                logger.info("Pcap handle closed.");
            }
        } catch (NotOpenException e) {
            logger.error("Error closing pcap handle", e);
        }
        shutdownExecutors();
    }

    private void shutdownExecutors() {
        captureExecutor.shutdown();
        processingExecutor.shutdown();

        try {
            if (!captureExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("captureExecutor did not terminate in time => forcing shutdownNow()");
                captureExecutor.shutdownNow();
            }

            if (!processingExecutor.awaitTermination(6, TimeUnit.SECONDS)) {
                logger.warn("processingExecutor did not terminate in time => forcing shutdownNow()");
                processingExecutor.shutdownNow();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("TrafficCaptureService shutdown complete.");
    }
}
