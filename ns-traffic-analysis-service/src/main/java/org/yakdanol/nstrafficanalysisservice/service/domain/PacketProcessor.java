package org.yakdanol.nstrafficanalysisservice.service.domain;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.yakdanol.nstrafficanalysisservice.service.domain.packet.FlowKey;
import org.yakdanol.nstrafficanalysisservice.service.domain.packet.FlowState;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class PacketProcessor {

    /** TTL «молчаливого» потока, сек.  */
    private static final long FLOW_TTL_SEC = 30;
    /** Максимум потоков одновременно. */
    private static final int  MAX_ACTIVE_FLOWS = 50_000;
    /** Максимальное количество сегментов, которое собираем на поток */
    public static final int MAX_SEGMENTS_PER_FLOW = 20;

    /* ==== STATE  ====================================================== */
    private final Map<FlowKey, FlowState> flows =
            new ConcurrentHashMap<>(16_384);

    /** отдаём наружу все обнаруженные домены за время анализа */
    public List<DomainHit> accept(Packet packet) {

        IpPacket  ip  = packet.get(IpPacket.class);
        TcpPacket tcp = packet.get(TcpPacket.class);

        if (ip == null || tcp == null) return Collections.emptyList();

        FlowKey key = new FlowKey(
                ip.getHeader().getSrcAddr().getHostAddress(),
                tcp.getHeader().getSrcPort().valueAsInt(),
                ip.getHeader().getDstAddr().getHostAddress(),
                tcp.getHeader().getDstPort().valueAsInt());

        // быстрый отбор «лишних» потоков
        if (flows.size() >= MAX_ACTIVE_FLOWS && !flows.containsKey(key)) {
            log.debug("Active-flow limit reached – skip {}", key);
            return Collections.emptyList();
        }

        FlowState state = flows.computeIfAbsent(key, k -> new FlowState(k, this::onSniFound));

        state.handleTcpSegment(
                tcp.getHeader().getSequenceNumber(),
                tcp.getPayload() != null
                        ? tcp.getPayload().getRawData()
                        : new byte[0]);

        // lazy-cleanup
        purgeExpiredFlows();

        // onSniFound() кладёт находки во внутренний bucket
        return state.drainNewHits();
    }

    /* ==== CALLBACK from FlowState ===================================== */
    private void onSniFound(FlowState st, String sni) {
        log.info("[SNI] {} -> {}", st.getFlowKey(), sni);
    }

    /* ==== GC dead flows =============================================== */
    private void purgeExpiredFlows() {
        long now = System.nanoTime();
        flows.entrySet().removeIf(e ->
                TimeUnit.NANOSECONDS.toSeconds(now - e.getValue().getLastSeen()) > FLOW_TTL_SEC);
    }
}

