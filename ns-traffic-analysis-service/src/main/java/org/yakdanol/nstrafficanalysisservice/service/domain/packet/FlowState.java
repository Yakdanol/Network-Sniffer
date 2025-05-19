package org.yakdanol.nstrafficanalysisservice.service.domain.packet;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yakdanol.nstrafficanalysisservice.service.domain.DomainHit;
import org.yakdanol.nstrafficanalysisservice.service.domain.PacketProcessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;

@Slf4j
public class FlowState {

    @Getter private final FlowKey flowKey;
    private final BiConsumer<FlowState,String> foundCallback;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private boolean sniExtracted = false;
    private long nextExpectedSeq = -1;
    private int segCount = 0;
    @Getter private volatile long lastSeen = System.nanoTime();

    /** thread-safe очередь новых DomainHit */
    private final Queue<DomainHit> freshHits = new ArrayDeque<>();

    public FlowState(FlowKey key, BiConsumer<FlowState, String> consumer) {
        this.flowKey = key;
        this.foundCallback = consumer;
    }

    public synchronized void handleTcpSegment(long seq, byte[] payload) {
        lastSeen = System.nanoTime();

        if (sniExtracted || segCount >= PacketProcessor.MAX_SEGMENTS_PER_FLOW)
            return;

        if (nextExpectedSeq < 0 || seq == nextExpectedSeq) {
            nextExpectedSeq  = seq + payload.length;
            segCount++;
            try {
                buf.write(payload);
            } catch (IOException ignored) { }

            String sni = TlsParser.extractSniFromTls(buf.toByteArray());
            if (sni != null) {
                sniExtracted = true;
                var hit = new DomainHit(
                        sni.toLowerCase(),
                        flowKey.getDstIp(),
                        LocalDateTime.now());
                freshHits.add(hit);
                foundCallback.accept(this, sni);
            }
        }
    }

    /** забираем накопленные DomainHit, queue -> caller */
    public synchronized List<DomainHit> drainNewHits() {
        List<DomainHit> list = new java.util.ArrayList<>(freshHits);
        freshHits.clear();
        return list;
    }
}
