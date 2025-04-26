package org.yakdanol.nstrafficanalysisservice.service.consumer;

import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;

@Service("filePacketConsumer")
public class FilePacketConsumer implements PacketConsumer {

    @Override
    public Packet getPacket() {
        return null;
    }
}
