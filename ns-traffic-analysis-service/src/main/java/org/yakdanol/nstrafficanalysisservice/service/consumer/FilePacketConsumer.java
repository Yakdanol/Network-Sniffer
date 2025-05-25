package org.yakdanol.nstrafficanalysisservice.service.consumer;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service("analysisFilePacketConsumer")
public class FilePacketConsumer implements PacketConsumer {
    private final static Logger logger = LoggerFactory.getLogger(FilePacketConsumer.class);
    private final TrafficAnalysisConfig trafficAnalysisConfig;
    private PcapHandle handle;

    public FilePacketConsumer(TrafficAnalysisConfig trafficAnalysisConfig) {
        this.trafficAnalysisConfig = trafficAnalysisConfig;
    }

    public void open(File file) throws PcapNativeException {
        this.handle = Pcaps.openOffline(file.getAbsolutePath());
    }

    @Override
    public Packet getPacket() {
        try {
            return handle.getNextPacket();
        } catch (NotOpenException e) {
            return null;
        }
    }

    @Override
    public List<Packet> getPackets() {
        if (handle == null) return List.of();
        int batchSize = trafficAnalysisConfig.getFileConfigs().getBatchSize();

        List<Packet> resultList = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            try {
                Packet packet = handle.getNextPacket();
                if (packet == null) break; // EOF
                resultList.add(packet);
            } catch (NotOpenException e) {
                logger.error("Pcap handle read error: {}", e.getMessage());
                break;
            }
        }

        return resultList;
    }

    @Override
    public void cancel() throws NotOpenException {
        if (handle != null) handle.breakLoop();
    }

    @Override
    public void close() {
        if (handle != null) handle.close();
    }
}
