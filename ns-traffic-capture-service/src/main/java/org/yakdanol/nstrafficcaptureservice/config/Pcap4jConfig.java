package org.yakdanol.nstrafficcaptureservice.config;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Pcap4jConfig {

    private final TrafficCaptureConfig config;

    @Autowired
    public Pcap4jConfig(TrafficCaptureConfig config) {
        this.config = config;
    }

    @Bean
    public PcapNetworkInterface networkInterface() throws Exception {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        return allDevs.stream()
                .filter(dev -> dev.getDescription().equals(config.getInterfaceName()))
                .findFirst()
                .orElseThrow(() -> new Exception("Network interface not found: " + config.getInterfaceName()));
    }
}
