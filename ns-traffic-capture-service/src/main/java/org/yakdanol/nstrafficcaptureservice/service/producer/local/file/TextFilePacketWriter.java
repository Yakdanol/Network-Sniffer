package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

public class TextFilePacketWriter extends FileAbstractLinePacketWriter {

    public TextFilePacketWriter(TrafficCaptureConfig config) throws Exception {
        super(config);
    }

    @Override
    protected String convertToOutputType(CapturedPacket capturedPacket) {
        return capturedPacket.toString();
    }
}
