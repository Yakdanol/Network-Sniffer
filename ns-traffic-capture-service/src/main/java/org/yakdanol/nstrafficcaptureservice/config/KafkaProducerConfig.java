package org.yakdanol.nstrafficcaptureservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final TrafficCaptureConfig trafficCaptureConfig;

    @Bean
    public Map<String, Object> producerConfigs() {
        TrafficCaptureConfig.KafkaProducerConfigs kafkaProducerConfigs = trafficCaptureConfig.getKafka();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerConfigs.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerConfigs.getRetries());
        props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProducerConfigs.getLingerMs());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProducerConfigs.getBatchSize());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProducerConfigs.getCompressionType());

        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);

        return props;
    }
}
