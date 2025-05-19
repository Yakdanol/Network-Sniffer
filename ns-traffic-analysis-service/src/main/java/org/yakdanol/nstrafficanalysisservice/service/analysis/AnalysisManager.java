package org.yakdanol.nstrafficanalysisservice.service.analysis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.service.domain.DomainHit;
import org.yakdanol.nstrafficanalysisservice.service.domain.PacketProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisManager {
    private final List<AnalysisHandler> handlers;

    @PostConstruct void init() throws Exception {
        for (AnalysisHandler h : handlers) h.preload();
    }

    public int detectThreats(List<DomainHit> hits,
                             String user,
                             Collection<DetectedThreat> bucket) {

        int fired = 0;
        for (var hit : hits) {
            for (AnalysisHandler handler : handlers) {
                if (handler.checkSecurity(hit.domain(), user)) {
                    bucket.add(new DetectedThreat(hit.domain(), handler.category(), hit.when()));
                    fired++;
                }
            }
        }
        return fired;
    }

    public record DetectedThreat(String domain, String category, LocalDateTime when) {}
}
