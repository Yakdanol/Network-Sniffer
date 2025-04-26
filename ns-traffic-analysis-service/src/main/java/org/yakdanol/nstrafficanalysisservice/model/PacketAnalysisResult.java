package org.yakdanol.nstrafficanalysisservice.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacketAnalysisResult {
    private String sourceIp;
    private String destinationIp;
    private String protocol;
    private String rawData;
}
