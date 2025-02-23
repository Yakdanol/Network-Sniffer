package org.yakdanol.nstrafficcaptureservice.service;

import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;

/**
 * Реализация PacketSender для локальной записи в файлы.
 */
@Service
@RequiredArgsConstructor
public class LocalFilePacketSender implements PacketSender {

    private final CapturedPacketRepository localFileRepository;

    @Override
    public void sendPacket(Packet packet) {
        localFileRepository.save(packet);
    }

    @Override
    public boolean checkAvailable() {
        return true;
    }
}
