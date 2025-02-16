package org.yakdanol.nstrafficcaptureservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CapturedPacket {

    private String timestamp;
    private String sourceIp;
    private String destinationIp;
    private String protocol;
    private int length;
    private String data;

    @Override
    public String toString() {
        return String.format(
                "{\"timestamp\":\"%s\",\"sourceIp\":\"%s\",\"destinationIp\":\"%s\",\"protocol\":\"%s\",\"length\":%d,\"data\":\"%s\"},",
                timestamp, sourceIp, destinationIp, protocol, length, data
        );
    }
}
