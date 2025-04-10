package org.yakdanol.nstrafficanalysisservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "traffic-analysis")
@Getter
@Setter
public class TrafficAnalysisConfig {
    // Общие поля
    private String processingMode; // Режим работы: "sequential" or "parallel"
    private String reportFormat; // Формат: отчета "xlsx", "pdf"
    private int poolSize;   // Runtime.getRuntime().availableProcessors() - 2
    private int batchSize;

    // Параметры для работы с файлами
    private FileConfig file = new FileConfig();
    // Параметры для работы с Kafka
    private KafkaConfig kafka = new KafkaConfig();

    @Getter
    @Setter
    public static class FileConfig {
        private String incomingFormat;  // json/xml/csv/text
        private String directory;       // "/data/analysis"
    }

    @Getter
    @Setter
    public static class KafkaConfig {
        private String topicName;
        private String bootstrapServers;
        private String groupId;
    }
}
