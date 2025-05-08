package org.yakdanol.nstrafficanalysisservice.service;

import jakarta.annotation.PreDestroy;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.service.consumer.PacketConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Сервис, который запускает цикл чтения пакетов из KafkaPacketConsumer
 * и выводит их в лог (консоль).
 */
@Service
public class TrafficPacketPrinterService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TrafficPacketPrinterService.class);

    private final PacketConsumer packetConsumer;
    private final ExecutorService executor;
    private volatile boolean running = true;

    public TrafficPacketPrinterService(@Qualifier("analysisKafkaPacketConsumer") PacketConsumer packetConsumer) {
        this.packetConsumer = packetConsumer;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "packet-printer-thread");
            t.setDaemon(false);
            return t;
        });
    }

    @Override
    public void run(ApplicationArguments args) {
        executor.submit(() -> {
            logger.info("TrafficPacketPrinterService started packet loop");
            while (running) {
                try {
                    Packet packet = packetConsumer.getPacket();
                    if (packet != null) {
                        logger.info("Received packet: {}", packet);
                    }
                } catch (Exception e) {
                    logger.error("Error fetching packet", e);
                    break;
                }
            }
            logger.info("TrafficPacketPrinterService stopped packet loop");
        });
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        executor.shutdownNow();
        logger.info("TrafficPacketPrinterService is shutting down");
    }
}

