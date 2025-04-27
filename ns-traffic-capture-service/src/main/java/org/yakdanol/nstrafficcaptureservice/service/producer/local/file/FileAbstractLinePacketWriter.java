package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.FilePacketWriter;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.LogFormat;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class FileAbstractLinePacketWriter implements FilePacketWriter {

    private static final Logger logger = LoggerFactory.getLogger(FileAbstractLinePacketWriter.class);

    private static final int BATCH_LINES = 20;
    private final List<String> batch = new ArrayList<>(BATCH_LINES);

    protected final TrafficCaptureConfig config;
    private final PacketToJsonConverter packetToJsonConverter = new PacketToJsonConverter();
    private BufferedWriter bufferedWriter;

    protected FileAbstractLinePacketWriter(TrafficCaptureConfig config) throws Exception {
        this.config = config;
        open();
    }

    protected abstract String convertToOutputType(CapturedPacket capturedPacket) throws Exception;

    @Override
    public void write(Packet packet) throws Exception {
        batch.add(convertToOutputType(packetToJsonConverter.convert(packet)));
        if (batch.size() >= BATCH_LINES) {
            flushBatch();
        }
    }

    @Override
    public void close() throws Exception {
        flushBatch();
        bufferedWriter.close();
    }

    private void flushBatch() throws Exception {
        for (String s : batch) {
            bufferedWriter.write(s + System.lineSeparator());
            // bufferedWriter.newLine();
        }
        batch.clear();
    }

    private void open() throws Exception {
        String outputTypeFile = getOutputType(config.getLogFormat());
        String file = "%s/%s.%s".formatted(config.getLogDirectory(), LocalDate.now(), outputTypeFile);
        bufferedWriter = new BufferedWriter(new FileWriter(file, true), 64 * 1024);
        logger.info("Opened {}", file);
    }

    private String getOutputType(LogFormat logFormat) {
        return switch (logFormat) {
            case TEXT -> "text";
            case XML -> "xml";
            case CSV -> "csv";
            default -> "json";
        };
    }
}
