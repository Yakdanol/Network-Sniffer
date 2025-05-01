package org.yakdanol.nstrafficanalysisservice.service.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Service("analysisKafkaPacketConsumer")
public class KafkaPacketConsumer implements PacketConsumer, InitializingBean, DisposableBean {
    private final static Logger logger = LoggerFactory.getLogger(KafkaPacketConsumer.class);
    private final TrafficAnalysisConfig trafficAnalysisConfigs;
    private final Consumer<String, byte[]> consumer;
    private volatile boolean initialized = false; // Флаг, показывающий, успешно ли инициализировали Kafka

    @Autowired
    public KafkaPacketConsumer(@Qualifier("consumerConfigs") Map<String, Object> consumerConfigs,
                               TrafficAnalysisConfig trafficAnalysisConfigs) {
        this.trafficAnalysisConfigs = trafficAnalysisConfigs;
        this.consumer = new KafkaConsumer<>(consumerConfigs);
    }

    @Override
    public void afterPropertiesSet() {
        String topic = trafficAnalysisConfigs.getKafkaConsumerConfigs().getTopicName();
        consumer.subscribe(Collections.singletonList(topic));
        initialized = true;
        logger.info("KafkaPacketConsumer: subscribed to topic \"{}\"", topic);
    }

    @Override
    public Packet getPacket() throws IllegalRawDataException {
        if (!initialized) {
            logger.error("getPacket() called before initialization");
            throw new IllegalStateException("KafkaPacketConsumer is not initialized");
        }

        int maxRetries = trafficAnalysisConfigs.getKafkaConsumerConfigs().getRetries();
        long retryDelay = trafficAnalysisConfigs.getKafkaConsumerConfigs().getRetryDelayMs();
        Duration pollTimeout = Duration.ofSeconds(trafficAnalysisConfigs.getKafkaConsumerConfigs().getCallbackTimeoutS());

        for (int attempt = 1; ; attempt++) {
            try {
                ConsumerRecords<String, byte[]> records = consumer.poll(pollTimeout);
                if (records.isEmpty()) {
                    logger.debug("No records received in poll, returning null");
                    return null;
                }

                for (ConsumerRecord<String, byte[]> record : records) {
                    byte[] raw = record.value();
                    if (raw == null || raw.length == 0) {
                        logger.warn("Empty payload at offset={} partition={}",
                                record.offset(), record.partition());
                        continue;
                    }

                    EthernetPacket eth = EthernetPacket.newPacket(raw, 0, raw.length);
                    logger.debug("Parsed EthernetPacket ({} bytes) offset={}", raw.length, record.offset());
                    // ручной коммит offset-а после успешной обработки
                    consumer.commitSync();
                    return eth;
                }

                // Если из всех записей ни одного корректного пакета
                return null;
            } catch (WakeupException e) {
                logger.info("Kafka consumer wakeup called, shutting down poll");
                throw e;
            } catch (IllegalRawDataException e) {
                logger.error("Byte array raw data parsing error");
                throw new IllegalRawDataException("Byte array raw data parsing error" + e.getMessage(), e);
            } catch (Exception e) {
                logger.warn("Poll error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage(), e);

                if (attempt >= maxRetries) {
                    logger.error("Exceeded maxRetries={}, throwing exception", maxRetries);
                    throw new RuntimeException("Kafka poll failed after retries", e);
                }

                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted during retry backoff", ie);
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
    }

    @Override
    public void destroy() {
        try {
            consumer.wakeup();
            consumer.close();
            logger.info("KafkaPacketConsumer closed successfully");
        } catch (Exception e) {
            logger.error("Error during Kafka consumer shutdown: {}", e.getMessage(), e);
        }
    }
}
