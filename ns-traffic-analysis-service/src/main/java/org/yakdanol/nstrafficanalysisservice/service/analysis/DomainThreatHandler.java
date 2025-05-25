package org.yakdanol.nstrafficanalysisservice.service.analysis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
import org.yakdanol.nstrafficanalysisservice.service.notification.NotificationPublisher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.stream.Stream;

/** Проверяет домены по категориям (файл <category>.txt) */
@Component
@RequiredArgsConstructor
public class DomainThreatHandler implements AnalysisHandler {

    private final TrafficAnalysisConfig cfg;
    private final NotificationPublisher publisher;
    private final RedisClient redisClient;
    private RedisCommands<String,String> redis;

    @PostConstruct void init() {
        redis = redisClient.connect().sync();
    }

    @Override public String category() {
        return "DOMAIN";
    }

    @Override
    public void preload() throws IOException, URISyntaxException {
        redis.flushdb();
        Path dir = Paths.get(getClass().getClassLoader()
                .getResource(cfg.getRedisConfigs()
                        .getDirectory())
                .toURI());
        // каждый .txt файл – своя категория
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path path : directoryStream) {
                String cat = path.getFileName().toString().replace(".txt","");
                try (Stream<String> lines = Files.lines(path)) {
                    redis.sadd(cat, lines.toArray(String[]::new));
                }
            }
        }
    }

    /** Проверяем домен — `data`=DOMAIN */
    @Override
    public boolean checkSecurity(String data, String internalUser) {
        // найдём первую категорию, куда входит домен
        for (String cat : redis.keys("*")) {
            if (redis.sismember(cat, data)) {
                publisher.publish(data, cat, internalUser);
                return true;
            }
        }
        return false;
    }
}
