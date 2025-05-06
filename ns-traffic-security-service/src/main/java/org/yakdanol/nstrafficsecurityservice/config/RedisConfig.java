package org.yakdanol.nstrafficsecurityservice.config;

import io.lettuce.core.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final TrafficSecurityConfig trafficSecurityConfig;

    @Bean
    public RedisClient redisClient() {
        TrafficSecurityConfig.RedisConfigs redisConfigs = trafficSecurityConfig.getRedisConfigs();
        return RedisClient.create(redisConfigs.getServerUrl());
    }
}
