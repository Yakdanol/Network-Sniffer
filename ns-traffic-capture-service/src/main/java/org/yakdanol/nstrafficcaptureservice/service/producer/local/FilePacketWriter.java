package org.yakdanol.nstrafficcaptureservice.service.producer.local;

import org.pcap4j.packet.Packet;

public interface FilePacketWriter extends AutoCloseable {

    void write(Packet packet) throws Exception;

    @Override
    void close() throws Exception;
}
