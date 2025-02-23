package org.yakdanol.nstrafficcaptureservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
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
        TrafficCaptureConfig.KafkaProperties kafkaProps = trafficCaptureConfig.getKafka();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProps.getRetries());
        props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProps.getLingerMs());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProps.getBatchSize());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProps.getCompressionType());

        return props;
    }
}
