package org.yakdanol.nstrafficanalysisservice.service.consumer;

import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;

@Service("analysisFilePacketConsumer")
public class FilePacketConsumer implements PacketConsumer {

    @Override
    public Packet getPacket() {
        return null;
    }
}
