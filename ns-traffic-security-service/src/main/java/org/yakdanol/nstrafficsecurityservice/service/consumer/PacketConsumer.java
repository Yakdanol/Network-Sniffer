package org.yakdanol.nstrafficsecurityservice.service.consumer;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;

import java.util.List;

public interface PacketConsumer extends AutoCloseable {
    Packet getPacket() throws IllegalRawDataException, NotOpenException;

    List<Packet> getPackets() throws IllegalRawDataException, NotOpenException;

    void cancel() throws NotOpenException;

    @Override
    void close();
}
