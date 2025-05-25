package org.yakdanol.nstrafficsecurityservice.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yakdanol.nstrafficsecurityservice.service.DataSource;

@Configuration
@ConfigurationProperties(prefix = "traffic-security")
@Getter
@Setter
@RequiredArgsConstructor
public class TrafficSecurityConfig {

    // Общие параметры работы микросервиса
    private GeneralConfig generalConfigs;
    // Параметры для работы с файлами
    private FileConfig fileConfigs;
    // Параметры для работы с Kafka
    private KafkaConsumerConfigs kafkaConsumerConfigs;
    // Параметры работы с Redis
    private RedisConfigs redisConfigs;

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class GeneralConfig {
        DataSource dataSource;
        private String processingMode;
        private int poolSize;
        private int queueSize;
        private int batchSize;
        private String reportFormat;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class FileConfig {
        private String directory;
        private int batchSize;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class KafkaConsumerConfigs {
        private String bootstrapServers;
        private String groupId;
        private String offsetReset;
        private String autoCommit;

        private String sessionTimeoutMs;
        private String heartbeatIntervalMs;
        private int lingerMs;
        private int batchSize;
        private int retries;
        private int retryDelayMs;
        private int callbackTimeoutS;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class RedisConfigs {
        private String directory;
        private String serverUrl;
    }
}
