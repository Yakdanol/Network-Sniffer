package org.yakdanol.nstrafficcaptureservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;
import org.yakdanol.nstrafficcaptureservice.service.KafkaPacketSender;
import org.yakdanol.nstrafficcaptureservice.service.LocalFilePacketSender;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.util.Map;

@Configuration
public class PacketSenderConfig {

    /**
     * LocalFilePacketSender -- создаётся всегда
     */
    @Bean(name = "localFilePacketSender")
    public LocalFilePacketSender localFilePacketSender(CapturedPacketRepository localFileRepository) {
        return new LocalFilePacketSender(localFileRepository);
    }

    /**
     * KafkaPacketSender -- создаётся только при mode = remote или mode = both
     * ConditionalOnExpression проверяет в SpEL, что mode - либо "remote", либо "both".
     */
    @Bean(name = "kafkaPacketSender")
    @ConditionalOnExpression("#{'${traffic-capture.mode}'.equalsIgnoreCase('remote') "
            + "or '${traffic-capture.mode}'.equalsIgnoreCase('both')}")
    public KafkaPacketSender kafkaPacketSender(
            @Qualifier("producerConfigs") Map<String, Object> producerConfigs,
            TrafficCaptureConfig config) {
        return new KafkaPacketSender(producerConfigs, config);
    }
}
