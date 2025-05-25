package org.yakdanol.nstrafficanalysisservice.config;

import io.lettuce.core.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final TrafficAnalysisConfig trafficAnalysisConfig;

    @Bean
    public RedisClient redisClient() {
        TrafficAnalysisConfig.RedisConfigs redisConfigs = trafficAnalysisConfig.getRedisConfigs();
        return RedisClient.create(redisConfigs.getServerUrl());
    }
}
