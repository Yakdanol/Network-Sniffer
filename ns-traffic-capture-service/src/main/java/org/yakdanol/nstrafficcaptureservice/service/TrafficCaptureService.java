package org.yakdanol.nstrafficcaptureservice.service;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class TrafficCaptureService {
    private final PcapHandle handle;
    private final CapturedPacketRepository repository;
    private final PacketToJsonConverter converter;
    private final ExecutorService executorService;
    private final FileRotationService fileRotationService;

    public TrafficCaptureService(PcapNetworkInterface networkInterface,
                                 CapturedPacketRepository repository,
                                 PacketToJsonConverter converter,
                                 FileRotationService fileRotationService) throws PcapNativeException, NotOpenException {
        this.handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
        // Установка фильтра для захвата нужных протоколов
        String filter = "tcp or udp or http or https or tls";
        handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
        this.repository = repository;
        this.converter = converter;
        this.executorService = Executors.newSingleThreadExecutor();
        this.fileRotationService = fileRotationService;
    }

    @PostConstruct
    public void startCapture() {
        executorService.submit(() -> {
            try {
                PacketListener listener = this::processPacket;
                handle.loop(-1, listener);
            } catch (InterruptedException e) {
                log.error("Packet capture interrupted", e);
                Thread.currentThread().interrupt();
            } catch (PcapNativeException | NotOpenException e) {
                log.error("Error in packet capture", e);
            }
        });

        // Запуск сервиса ротации файлов
        fileRotationService.start();
    }

    private void processPacket(Packet packet) {
        try {
            CapturedPacket capturedPacket = converter.convert(packet);
            repository.save(capturedPacket);
        } catch (Exception e) {
            log.error("Error processing packet", e);
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
        executorService.shutdownNow();
        fileRotationService.stop();
    }
}
