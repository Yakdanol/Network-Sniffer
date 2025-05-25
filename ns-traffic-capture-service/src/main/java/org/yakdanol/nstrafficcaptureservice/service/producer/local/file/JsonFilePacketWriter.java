package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

public class JsonFilePacketWriter extends FileAbstractLinePacketWriter {

    private final ObjectMapper objectMapper;

    public JsonFilePacketWriter(TrafficCaptureConfig config) throws Exception {
        super(config);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected String convertToOutputType(CapturedPacket capturedPacket) throws JsonProcessingException {
        return objectMapper.writeValueAsString(capturedPacket);
    }
}
