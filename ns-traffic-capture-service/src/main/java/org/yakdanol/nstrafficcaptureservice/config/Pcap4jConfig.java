package org.yakdanol.nstrafficcaptureservice.config;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Pcap4jConfig {

    @Value("${traffic-capture.interface-name}")
    private String interfaceName;

    @Bean
    public PcapNetworkInterface networkInterface() throws Exception {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        return allDevs.stream()
                .filter(dev -> dev.getDescription().equals(interfaceName))
                .findFirst()
                .orElseThrow(() -> new Exception("Network interface not found: " + interfaceName));
    }
}
