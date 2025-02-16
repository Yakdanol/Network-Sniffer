package org.yakdanol.nstrafficcaptureservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "traffic-capture")
public class TrafficCaptureConfig {

    // Общие поля
    private String user;
    private String system;
    private String mode;
    private String interfaceName;
    private String logDirectory;
    private String logFormat;
    private String filter;
    private int capturingPoolSize;
    private int processingPoolSize;
    private int queueSize;
    private boolean consoleLogging;

    // Настройки Kafka
    private KafkaProperties kafka = new KafkaProperties();

    @Getter
    @Setter
    public static class KafkaProperties {
        private String bootstrapServers;
        private String topicName;
        private int retries;
        private int lingerMs;
        private int batchSize;
        private String compressionType;
        private int healthCheckTimeoutMs;
    }
}
