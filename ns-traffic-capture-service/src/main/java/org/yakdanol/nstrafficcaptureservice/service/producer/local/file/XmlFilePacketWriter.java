package org.yakdanol.nstrafficcaptureservice.service.producer.local.file;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

public class XmlFilePacketWriter extends FileAbstractLinePacketWriter {

    private final ObjectWriter objectWriter;

    public XmlFilePacketWriter(TrafficCaptureConfig config) throws Exception {
        super(config);
        objectWriter = new XmlMapper().writer();
    }

    @Override
    protected String convertToOutputType(CapturedPacket capturedPacket) throws Exception {
        return objectWriter.writeValueAsString(capturedPacket);
    }
}
