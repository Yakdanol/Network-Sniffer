package org.yakdanol.nstrafficsecurityservice.service.threat.handlers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficsecurityservice.service.notification.NotificationPublisher;
import org.yakdanol.nstrafficsecurityservice.service.threat.ThreatHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AdvertisingThreatHandler implements ThreatHandler {

    private static final String CATEGORY = "ADVERTISING";
    private static final Path FILE = Path.of("C:\\Users\\Yakdanol\\Desktop\\Java\\Diploma\\Network-Sniffer\\ns-traffic-security-service\\src\\main\\resources\\data\\IPs\\advertising-IPs.txt");
//    private static final Path FILE = Path.of("data", "IPs", "advertising-IPs.txt");
    private final RedisClient redisClient;
    private RedisCommands<String, String> redis;
    private final NotificationPublisher publisher;

    @PostConstruct
    public void init() {
        this.redis = redisClient.connect().sync();
    }

    @Override public String category() {
        return CATEGORY;
    }

    @Override
    public void preload() throws IOException {
        redis.del(CATEGORY);
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
