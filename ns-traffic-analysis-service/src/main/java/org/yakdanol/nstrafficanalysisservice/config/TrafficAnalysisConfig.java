package org.yakdanol.nstrafficanalysisservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yakdanol.nstrafficanalysisservice.service.DataSource;

@Configuration
@ConfigurationProperties(prefix = "traffic-analysis")
@Getter
@Setter
public class TrafficAnalysisConfig {

    // Общие поля
    DataSource dataSource;
    private String processingMode;
    private int poolSize;
    private int batchSize;
    private String reportFormat;

    // Параметры для работы с файлами
    private FileConfig fileConfig = new FileConfig();
    // Параметры для работы с Kafka
    private KafkaConsumerConfigs kafkaConsumerConfigs = new KafkaConsumerConfigs();

    @Getter
    @Setter
    public static class FileConfig {
        private String incomingFormat;
        private String directory;
    }

    @Getter
    @Setter
    public static class KafkaConsumerConfigs {
        private String bootstrapServers;
        private String topicName;
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
}
