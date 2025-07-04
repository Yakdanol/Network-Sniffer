package org.yakdanol.nstrafficcaptureservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yakdanol.nstrafficcaptureservice.service.producer.WorkingMode;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.LogFormat;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "traffic-capture")
public class TrafficCaptureConfig {

    // Общие поля
    private String user;
    private String system;
    private WorkingMode mode;
    private String interfaceName;
    private String logDirectory;
    private LogFormat logFormat;
    private String filter;
    private int capturingPoolSize;
    private int processingPoolSize;
    private int queueSize;
    private boolean consoleLogging;

    // Настройки Kafka
    private KafkaProducerConfigs kafka = new KafkaProducerConfigs();

    @Getter
    @Setter
    public static class KafkaProducerConfigs {
        private String bootstrapServers;
        private String topicName;
        private int lingerMs;
        private int batchSize;
        private String compressionType;

        private int requestTimeoutMs;
        private int maxBlock;
        private int retries;
        private int retryDelayMs;
        private int callbackTimeoutS;
        private int healthCheckTimeoutMs;
    }
}
