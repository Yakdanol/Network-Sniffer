package org.yakdanol.nstrafficcaptureservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yakdanol.nstrafficcaptureservice.service.producer.kafka.KafkaPacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.local.LocalFilePacketSender;

import java.util.Map;

@Configuration
public class PacketSenderConfig {

    /**
     * LocalFilePacketSender -- создаётся при mode = local или mode = local_and_remote
     */
    @Bean(name = "localFilePacketSender")
    @ConditionalOnExpression("T(java.util.Arrays).asList('LOCAL','LOCAL_AND_REMOTE').contains('${traffic-capture.mode}'.toUpperCase())")
    public LocalFilePacketSender localFilePacketSender(TrafficCaptureConfig config) throws Exception {
        return new LocalFilePacketSender(config);
    }

    /**
     * KafkaPacketSender -- создаётся только при mode = remote или mode = local_and_remote
     */
    @Bean(name = "kafkaPacketSender")
    @ConditionalOnExpression("T(java.util.Arrays).asList('REMOTE','LOCAL_AND_REMOTE').contains('${traffic-capture.mode}'.toUpperCase())")
    public KafkaPacketSender kafkaPacketSender(
            @Qualifier("producerConfigs") Map<String, Object> producerConfigs,
            TrafficCaptureConfig config) {
        return new KafkaPacketSender(producerConfigs, config);
    }
}
