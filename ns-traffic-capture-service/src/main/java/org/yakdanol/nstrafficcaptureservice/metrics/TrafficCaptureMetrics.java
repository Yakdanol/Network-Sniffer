package org.yakdanol.nstrafficcaptureservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TrafficCaptureMetrics {

    private final Counter capturedPacketsCounter;
    private final Counter processingErrorsCounter;

    public TrafficCaptureMetrics(MeterRegistry meterRegistry) {
        this.capturedPacketsCounter = meterRegistry.counter("trafficcapture.packets.total", "type", "captured");
        this.processingErrorsCounter = meterRegistry.counter("trafficcapture.errors.total", "type", "processing");
    }

    public void incrementCapturedPackets() {
        capturedPacketsCounter.increment();
    }

    public void incrementProcessingErrors() {
        processingErrorsCounter.increment();
    }
}
