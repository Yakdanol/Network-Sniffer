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
import org.yakdanol.nstrafficcaptureservice.service.producer.kafka.KafkaPacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.LocalFilePacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.PacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.WorkingMode;

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

    @Autowired
    public TrafficCaptureService(TrafficCaptureConfig config,
                                 PcapNetworkInterface networkInterface,
                                 MeterRegistry meterRegistry,
                                 @Qualifier("kafkaPacketSender") KafkaPacketSender kafkaSender,
                                 @Qualifier("localFilePacketSender") LocalFilePacketSender localFileSender) throws PcapNativeException, NotOpenException {
        this.config = config;
        this.metrics = new TrafficCaptureMetrics(meterRegistry);

        this.kafkaSender = kafkaSender;
        this.localFileSender = localFileSender;

        this.handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS, 50);
        this.handle.setFilter(config.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

        this.captureExecutor = Executors.newFixedThreadPool(config.getCapturingPoolSize());
        this.processingExecutor = Executors.newFixedThreadPool(config.getProcessingPoolSize());
        this.packetQueue = new LinkedBlockingQueue<>(config.getQueueSize());

        logger.info("TrafficCaptureService constructor: pcap handle opened, filter={}, mode={}", config.getFilter(), config.getMode());
    }

    @PostConstruct
    public void init() {
        // Определяем тип работы сервиса (Local / Remote / Local_and_Remote)
        WorkingMode mode = config.getMode();
        logger.info("TrafficCaptureService starting in mode: {}", mode);

        switch (mode) {
            case REMOTE -> {
                if (kafkaSender.checkAvailable()) {
                    activeSender = kafkaSender;
                    doubleWrite = false;
                    logger.info("Kafka is available. Using remote mode only with Kafka.");
                } else {
                    activeSender = localFileSender;
                    doubleWrite = false;
                    logger.warn("Kafka is NOT available. Fallback to local mode.");
                }
            }
            case LOCAL_AND_REMOTE -> {
                if (kafkaSender.checkAvailable()) {
                    activeSender = kafkaSender;
                    doubleWrite = true;
                    logger.info("Kafka is available. Using double-write with Local and Remote mode.");
                } else {
                    activeSender = localFileSender;
                    doubleWrite = false;
                    logger.warn("Kafka is NOT available. Fallback to local mode (no double-write).");
                }
            }
            case LOCAL -> {
                activeSender = localFileSender;
                doubleWrite = false;
                logger.info("Local mode enabled. Will only write to local file.");
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
            // Если не удалось открыть сетевой интерфейс - завершаем работу
            shutdown();
//            shutdownExecutors();
        }
    }

    public void startCapture() {
        captureExecutor.submit(() -> {
            try {
                PacketListener listener = this::enqueuePacket; // TODO: Попробовать заменить сразу на sendPacketWithFallback
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

    private void sendPacketWithFallback(Packet packet) throws Exception {
        if (doubleWrite) {
            try {
                kafkaSender.sendPacket(packet);
            } catch (Exception ex) {
                logger.error("Kafka send failed in 'both' mode. Fallback partial: we still have local file. Error={}", ex.getMessage());
                doubleWrite = false;
                activeSender = localFileSender;
                localFileSender.sendPacket(packet);
                kafkaSender.closeProducer();
                return;
            }

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

            if (!processingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("processingExecutor did not terminate in time => forcing shutdownNow()");
                processingExecutor.shutdownNow();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("TrafficCaptureService shutdown complete.");
    }
}
