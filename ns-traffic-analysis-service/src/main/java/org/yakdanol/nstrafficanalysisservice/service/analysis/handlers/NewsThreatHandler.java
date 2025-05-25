package org.yakdanol.nstrafficanalysisservice.service.analysis.handlers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
import org.yakdanol.nstrafficanalysisservice.service.analysis.AnalysisHandler;
import org.yakdanol.nstrafficanalysisservice.service.notification.NotificationPublisher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class NewsThreatHandler implements AnalysisHandler {
    private final TrafficAnalysisConfig configs;
    private static final String CATEGORY = "NEWS";
    private final RedisClient redisClient;
    private RedisCommands<String, String> redis;
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
        redis.del(CATEGORY);
        URL path = getClass().getClassLoader().getResource(configs.getRedisConfigs().getDirectory() + "news.txt");
        Path FILE = Paths.get(path.toURI());
        try (Stream<String> lines = Files.lines(FILE)) {
            redis.sadd(CATEGORY, lines.toArray(String[]::new));
        }
    }

    @Override
    public boolean checkSecurity(Packet packet, String internalUserName) {
        if (!packet.contains(IpV4Packet.class)) return false;
        String destinationIp = packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress();
        if (redis.sismember(CATEGORY, destinationIp)) {
            publisher.publish(destinationIp, CATEGORY, internalUserName);
            return true;
        }
        return false;
    }
}
