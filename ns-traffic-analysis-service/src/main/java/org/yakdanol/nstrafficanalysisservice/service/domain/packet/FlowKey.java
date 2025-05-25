package org.yakdanol.nstrafficanalysisservice.service.domain.packet;

import lombok.*;

import java.util.Objects;

/**
 * Ключ, уникально идентифицирующий поток TCP
 * по (srcIp, srcPort, dstIp, dstPort).
 */
@Getter
@Setter
@AllArgsConstructor
public class FlowKey {
    private final String srcIp;
    private final int srcPort;
    private final String dstIp;
    private final int dstPort;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FlowKey)) return false;
        FlowKey flowKey = (FlowKey) object;
        return srcPort == flowKey.srcPort
                && dstPort == flowKey.dstPort
                && Objects.equals(srcIp, flowKey.srcIp)
                && Objects.equals(dstIp, flowKey.dstIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIp, srcPort, dstIp, dstPort);
    }

    @Override
    public String toString() {
        return srcIp + ":" + srcPort + " -> " + dstIp + ":" + dstPort;
    }
}
