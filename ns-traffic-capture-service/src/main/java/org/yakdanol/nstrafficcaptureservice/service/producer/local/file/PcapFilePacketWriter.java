package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.FilePacketWriter;

import java.time.LocalDate;

public class PcapFilePacketWriter implements FilePacketWriter {
    private static final Logger logger = LoggerFactory.getLogger(PcapFilePacketWriter.class);

    private final TrafficCaptureConfig config;
    private PcapHandle handle;
    private PcapDumper dumper;

    public PcapFilePacketWriter(TrafficCaptureConfig config) throws Exception {
        this.config = config;
        open();
    }

    @Override
    public void write(Packet pkt) {
        try {
            dumper.dump(pkt);
        } catch (Exception e) {
            logger.error("Error writing packet to file: {}", e.getMessage(), e);
        }
    }

    private void open() throws Exception {
        String logDirectory = config.getLogDirectory();
        LocalDate currentDay =  LocalDate.now();
        String path = "%s/%s.pcap".formatted(logDirectory, currentDay);
        handle = Pcaps.openDead(DataLinkType.EN10MB, 65536);
        dumper = handle.dumpOpen(path);
        logger.info("Opened pcap file {}", path);
    }

    @Override
    public void close() {
        try {
            if (dumper != null) {
                dumper.flush();
                dumper.close();
            }
            if (handle != null && handle.isOpen()) {
                handle.close();
            }
        } catch (Exception e) {
            logger.error("Error closing pcap file: {}", e.getMessage(), e);
        }
    }
}
