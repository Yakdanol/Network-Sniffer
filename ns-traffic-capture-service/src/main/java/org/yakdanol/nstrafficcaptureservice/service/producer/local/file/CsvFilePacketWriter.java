package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

public class CsvFilePacketWriter extends FileAbstractLinePacketWriter {

    private final ObjectWriter objectWriter;

    public CsvFilePacketWriter(TrafficCaptureConfig config) throws Exception {
        super(config);
        objectWriter = new CsvMapper().writer().forType(CapturedPacket.class);
    }

    @Override
    protected String convertToOutputType(CapturedPacket capturedPacket) throws Exception {
        return objectWriter.writeValueAsString(capturedPacket);
    }
}
