package org.yakdanol.nstrafficanalysisservice.service.domain;

import java.time.LocalDateTime;

public record DomainHit(String domain, String dstIp, LocalDateTime when) {}
