package org.yakdanol.nstrafficsecurityservice.service.threat;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThreatManager {

    private final List<ThreatHandler> handlers;

    /** Инициализация при старте, все обработчики загружают данные из txt‑файлов в Redis. */
    @PostConstruct
    void init() throws IOException, URISyntaxException {
        for (ThreatHandler handler : handlers) handler.preload();
    }

    /** @return кол-во сработавших обработчиков (для метрик). */
    public int detectThreats(List<Packet> packets, String internalUserName, Collection<DetectedThreat> bucket) {
        int countDetectedDanger = 0;
        for (Packet packet : packets) {
            for (ThreatHandler handler : handlers) {
                if (handler.checkSecurity(packet, internalUserName)) {
                    bucket.add(new DetectedThreat(
                            packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress(),
                            handler.category(), LocalDateTime.now()));
                    countDetectedDanger++;
                }
            }
        }

        return countDetectedDanger;
    }

    /** DTO для PDF‑отчёта. */
    public record DetectedThreat(String data, String category, LocalDateTime when) {}
}
