package org.yakdanol.nstrafficsecurityservice.service.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficsecurityservice.config.TrafficSecurityConfig;

import java.time.Duration;
import java.util.*;

@Service("securityKafkaPacketConsumer")
public class KafkaPacketConsumer implements PacketConsumer {
    private final static Logger logger = LoggerFactory.getLogger(KafkaPacketConsumer.class);
    private final TrafficSecurityConfig trafficSecurityConfig;

    private final KafkaConsumer<String, byte[]> consumer;
    private volatile boolean stopped = false;

    @Autowired
    public KafkaPacketConsumer(@Qualifier("consumerConfigs") Map<String, Object> consumerConfigs,
                               TrafficSecurityConfig trafficSecurityConfig) {
        this.trafficSecurityConfig = trafficSecurityConfig;
        this.consumer = new KafkaConsumer<>(consumerConfigs);
    }

    public void subscribe(String topic) {
        logger.info("KafkaPacketConsumer: subscribed to topic \"{}\"", topic);
        consumer.subscribe(List.of(topic));
    }

    @Override
    public List<Packet> getPackets() throws IllegalRawDataException {

        try {
            int maxRetries = trafficSecurityConfig.getKafkaConsumerConfigs().getRetries();
            long retryDelay = trafficSecurityConfig.getKafkaConsumerConfigs().getRetryDelayMs();
            Duration pollTimeout = Duration.ofSeconds(trafficSecurityConfig.getKafkaConsumerConfigs().getCallbackTimeoutS());

            ConsumerRecords<String, byte[]> records = consumer.poll(pollTimeout);
            if (records.isEmpty()) return List.of();

            List<Packet> resultList = new ArrayList<>(records.count());
            for (ConsumerRecord<String, byte[]> record : records) {
                byte[] raw = record.value();
                resultList.add(EthernetPacket.newPacket(raw, 0, raw.length));
            }
            consumer.commitSync();

            return resultList;
        } catch (WakeupException ex) {
            if (stopped) throw ex; // close() вызван
            return List.of();
        }
    }

    @Override
    public Packet getPacket() throws IllegalRawDataException {

        int maxRetries = trafficSecurityConfig.getKafkaConsumerConfigs().getRetries();
        long retryDelay = trafficSecurityConfig.getKafkaConsumerConfigs().getRetryDelayMs();
        Duration pollTimeout = Duration.ofSeconds(trafficSecurityConfig.getKafkaConsumerConfigs().getCallbackTimeoutS());

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

    public void unsubscribe() {
        logger.info("KafkaPacketConsumer: unsubscribed from topic");
        consumer.unsubscribe();
    }

    @Override
    public void cancel() {
        try {
            logger.info("Trying to stop Task and close KafkaPacketConsumer");
            consumer.wakeup();
        } catch (Exception e) {
            logger.error("Error during Kafka consumer shutdown: {}", e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            stopped = true;
            consumer.wakeup();
            consumer.close();
            logger.info("KafkaPacketConsumer closed successfully");
        } catch (Exception e) {
            logger.error("Error during Kafka consumer shutdown: {}", e.getMessage(), e);
        }
    }
}
