package org.yakdanol.nstrafficsecurityservice.service.threat.handlers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficsecurityservice.config.TrafficSecurityConfig;
import org.yakdanol.nstrafficsecurityservice.service.notification.NotificationPublisher;
import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class
RegionHandler implements ThreatHandler {
    private final TrafficSecurityConfig configs;
    private static final String CATEGORY = "REGION";
    private final RedisClient redisClient;
    private RedisCommands<String, String> redis;
    private Searcher searcher;
    private final NotificationPublisher publisher;

    @PostConstruct
    public void init() {
        this.redis = redisClient.connect().sync();
    }

    @Override
    public String category() {
        return CATEGORY;
    }

    @Override
    public void preload() throws IOException, URISyntaxException {
        // RegionDB load
        URL path = getClass().getClassLoader().getResource(configs.getRedisConfigs().getDirectory() + "ip2region.xdb");
        Path FILE = Paths.get(path.toURI());
        this.searcher = Searcher.newWithFileOnly(FILE.toString());

        // 2) Загрузка регионов в Redis
        redis.del(CATEGORY);
        URL regionsUrl = getClass().getClassLoader()
                .getResource(configs.getRedisConfigs().getDirectory() + "regions.txt");
        Path regionsPath = Paths.get(regionsUrl.toURI());
        try (Stream<String> lines = Files.lines(regionsPath)) {
            // lines — уже в формате Country|Region
            String[] entries = lines
                    .filter(line -> !line.isBlank())
                    .toArray(String[]::new);
            if (entries.length > 0) {
                redis.sadd(CATEGORY, entries);
            }
        }
    }

    @Override
    public boolean checkSecurity(Packet packet, String internalUserName) {
        if (!packet.contains(IpV4Packet.class)) return false;
        String destinationIp = packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress();
        try {
            String fullRegion = searcher.search(destinationIp);
            if (fullRegion == null || fullRegion.isEmpty()) {
                return false;
            }

            // fullRegion = "Country|Region|Province|City|ISP"
            String[] parts = fullRegion.split("\\|", -1);
            String country = parts[0];
            String region  = parts[1];
            String key     = country + "|" + region;

            if (redis.sismember(CATEGORY, key)) {
                publisher.publish(destinationIp, CATEGORY, internalUserName);
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }
}
