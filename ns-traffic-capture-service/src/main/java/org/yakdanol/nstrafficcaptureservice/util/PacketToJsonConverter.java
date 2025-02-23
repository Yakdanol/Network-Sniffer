package org.yakdanol.nstrafficcaptureservice.util;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PacketToJsonConverter {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CapturedPacket convert(Packet packet) {
        CapturedPacket capturedPacket = new CapturedPacket();
        capturedPacket.setTimestamp(LocalDateTime.now().format(formatter));

        if (packet.contains(IpPacket.class)) {
            IpPacket ipPacket = packet.get(IpPacket.class);
            capturedPacket.setSourceIp(ipPacket.getHeader().getSrcAddr().getHostAddress());
            capturedPacket.setDestinationIp(ipPacket.getHeader().getDstAddr().getHostAddress());
            capturedPacket.setProtocol(ipPacket.getHeader().getProtocol().name());
        } else {
            capturedPacket.setSourceIp("Unknown");
            capturedPacket.setDestinationIp("Unknown");
            capturedPacket.setProtocol("Unknown");
        }

        capturedPacket.setLength(packet.length());
        capturedPacket.setData(packet.toString());

        return capturedPacket;
    }
}
