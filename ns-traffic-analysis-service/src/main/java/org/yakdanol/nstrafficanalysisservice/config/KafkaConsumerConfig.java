package org.yakdanol.nstrafficanalysisservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private final TrafficAnalysisConfig trafficAnalysisConfig;

    @Bean
    public Map<String, Object> consumerConfigs() {
        TrafficAnalysisConfig.KafkaConfig kafkaConfigs = trafficAnalysisConfig.getKafka();

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigs.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfigs.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, trafficAnalysisConfig.getBatchSize());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfigs.getAutoCommit());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfigs.getOffsetReset());
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, kafkaConfigs.getRetryDelayMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaConfigs.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, kafkaConfigs.getHeartbeatIntervalMs());

        return props;
    }
}
