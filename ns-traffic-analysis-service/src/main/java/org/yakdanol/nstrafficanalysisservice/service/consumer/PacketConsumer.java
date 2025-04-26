package org.yakdanol.nstrafficanalysisservice.service.consumer;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;

public interface PacketConsumer {
    public Packet getPacket() throws IllegalRawDataException;
}
