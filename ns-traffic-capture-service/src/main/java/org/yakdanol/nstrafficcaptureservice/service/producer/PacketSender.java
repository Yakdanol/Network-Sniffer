package org.yakdanol.nstrafficcaptureservice.service.producer;

import org.pcap4j.packet.Packet;

/**
 * Интерфейс для отправки пакетов трафика во внешнюю систему или локальное хранилище
 */
public interface PacketSender {

    void sendPacket(Packet packet) throws Exception;

    boolean checkAvailable();
}
