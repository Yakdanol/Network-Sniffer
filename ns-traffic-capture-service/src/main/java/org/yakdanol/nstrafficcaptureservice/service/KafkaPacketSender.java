package org.yakdanol.nstrafficcaptureservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.pcap4j.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.model.CapturedPacket;
import org.yakdanol.nstrafficcaptureservice.util.PacketToJsonConverter;

import java.util.Map;

/**
 * Класс отправки пакетов в Kafka.
 * 1) При старте (конструктор) пробуем создать producer c короткими таймаутами,
 *    один раз проверяем partitionsFor.
 * 2) Запускаем отдельный поток (kafkaMonitorThread), который каждые N секунд
 *    проверяет доступность Kafka. Если недоступно X раз подряд -> closeProducer().
 * 3) Если producer=null или kafkaAvailable=false, при вызове sendPacket бросается исключение,
 *    что приведёт к fallback в TrafficCaptureService.
 */
@Service("kafkaPacketSender")
public class KafkaPacketSender implements PacketSender {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPacketSender.class);

    private Producer<String, String> producer;
    private final TrafficCaptureConfig config;
    private final ObjectMapper objectMapper;
    private final PacketToJsonConverter converter;
    private final String topicName;

    // Флаг, показывающий, успешно ли инициализировали Kafka
    private volatile boolean kafkaAvailable;

    public KafkaPacketSender(@Qualifier("producerConfigs") Map<String, Object> producerConfigs,
                             TrafficCaptureConfig config,
                             PacketToJsonConverter converter) {
        this.config = config;
        this.converter = converter;
        this.objectMapper = new ObjectMapper();
        this.topicName = config.getKafka().getTopicName() + "." + config.getUser();
        this.kafkaAvailable = false;

        // Если режим local => не создаём реальный KafkaProducer
        String mode = config.getMode();
        if ("local".equalsIgnoreCase(mode)) {
            logger.info("KafkaPacketSender: mode=local => skipping KafkaProducer creation.");
            this.producer = null;
        } else {
            try {
                // Принудительно прописываем параметры, чтобы не блокироваться навсегда
                producerConfigs.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
                producerConfigs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);

                logger.info("KafkaPacketSender: Trying to create KafkaProducer for mode={}", mode);
                this.producer = new KafkaProducer<>(producerConfigs);
                // Успешно создали, проверяем доступность
                boolean ok = checkAvailable();
                if (ok) {
                    kafkaAvailable = true;
                    logger.info("KafkaPacketSender: Kafka is initially available.");
                } else {
                    logger.warn("KafkaPacketSender: Kafka is NOT available at init, producer set to null.");
                    closeProducer();
                }
            } catch (Exception e) {
                logger.error("KafkaPacketSender: Failed to init producer, cause: {}", e.getMessage(), e);
                closeProducer();
            }
        }
    }

    public boolean checkAvailable() {
        if (producer == null) {
            return false;
        }

        try {
            producer.partitionsFor(topicName);
            return true;
        } catch (Exception e) {
            logger.error("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void sendPacket(Packet packet) throws Exception {
        // Если producer=null или kafka недоступна => ничего не делаем
        if (!kafkaAvailable || producer == null) {
            throw new IllegalStateException("KafkaProducer is null (mode=local). Can't send packet to Kafka.");
        }

        // Преобразуем пакет в JSON и отправляем
        CapturedPacket capturedPacket = converter.convert(packet);
        String message = objectMapper.writeValueAsString(capturedPacket);
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, null, message);

        // Отправка (асинхронно)
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.error("Kafka send failed for packet: {}", exception.getMessage());
            }
        });
    }

    /**
     * Закрыть продюсер и пометить Kafka как недоступную.
     */
    public void closeProducer() {
        kafkaAvailable = false;
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
                logger.info("Kafka producer closed");
            } catch (Exception e) {
                logger.error("Error closing producer => {}", e.getMessage());
            }
            producer = null;
        }
    }
}
