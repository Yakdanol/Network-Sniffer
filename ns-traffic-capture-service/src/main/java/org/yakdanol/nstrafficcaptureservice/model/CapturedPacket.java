package org.yakdanol.nstrafficcaptureservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class CapturedPacket {

    private String timestamp;

    private String sourceIp;

    private String destinationIp;

    private String protocol;

    private int length;

    private String data;
}
