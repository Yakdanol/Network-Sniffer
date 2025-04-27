package org.yakdanol.nstrafficcaptureservice.service.producer.local;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.service.producer.PacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.file.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Класс локальной записи пакетов трафика в файлы в заданном формате
 */
@Service
@RequiredArgsConstructor
public class LocalFilePacketSender implements PacketSender {
    private static final Logger logger = LoggerFactory.getLogger(LocalFilePacketSender.class);
    private final FilePacketWriter filePacketWriter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    public LocalFilePacketSender(TrafficCaptureConfig config) throws Exception {
        filePacketWriter = switch (config.getLogFormat()) {
            case PCAP -> new PcapFilePacketWriter(config);
            case TEXT -> new TextFilePacketWriter(config);
            case XML -> new XmlFilePacketWriter(config);
            case JSON -> new JsonFilePacketWriter(config);
            case CSV -> new CsvFilePacketWriter(config);
        };
    }

    @Override
    public void sendPacket(Packet packet) {
        executorService.execute(() -> {
            try {
                filePacketWriter.write(packet);
            } catch (Exception e) {
                logger.error("File write error", e); }
        });
    }

    @Override
    public boolean checkAvailable() {
        return true;
    }

    @PreDestroy
    public void shutdown() throws Exception {
        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) executorService.shutdownNow();
        filePacketWriter.close();
        logger.info("LocalFilePacketSender stopped");
    }
}
