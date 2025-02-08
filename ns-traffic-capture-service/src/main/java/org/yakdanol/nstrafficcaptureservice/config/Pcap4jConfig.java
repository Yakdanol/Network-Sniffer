package org.yakdanol.nstrafficcaptureservice.config;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
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
        for (PcapNetworkInterface dev : allDevs) {
            if (dev.getDescription() != null && dev.getDescription().equals(config.getInterfaceName())) {
                return dev;
            }
        }

        // Если указанный в конфигурациях интерфейс не найден, выбираем автоматически
        return allDevs.stream()
                .max(Comparator.comparingInt(dev -> dev.getAddresses().size()))
                .orElseThrow(() -> new Exception("No suitable network interface found."));
    }
}
