package org.yakdanol.nstrafficcaptureservice.service;

import org.pcap4j.packet.Packet;


// Общий интерфейс для отправки/сохранения пакетов.
public interface PacketSender {
    void sendPacket(Packet packet) throws Exception;

    boolean checkAvailable();
}
