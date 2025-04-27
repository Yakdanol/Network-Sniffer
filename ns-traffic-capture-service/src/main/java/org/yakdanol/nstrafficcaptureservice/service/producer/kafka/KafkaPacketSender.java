package org.yakdanol.nstrafficcaptureservice.service.producer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficcaptureservice.config.TrafficCaptureConfig;
import org.yakdanol.nstrafficcaptureservice.service.producer.PacketSender;
import org.yakdanol.nstrafficcaptureservice.service.producer.WorkingMode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Класс отправки пакетов в Kafka.
 * 1) При старте (конструктор) пробуем создать producer c короткими таймаутами,
 * один раз проверяем partitionsFor.
 * 2) Запускаем отдельный поток (kafkaMonitorThread), который каждые N секунд
 * проверяет доступность Kafka. Если недоступно X раз подряд -> closeProducer().
 * 3) Если producer = null или kafkaAvailable = false, при вызове sendPacket бросается исключение,
 * что приведёт к fallback в TrafficCaptureService.
 */
@Service("kafkaPacketSender")
public class KafkaPacketSender implements PacketSender {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPacketSender.class);

    private final TrafficCaptureConfig config;
    private final String topicName;

    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private volatile boolean kafkaAvailable; // Флаг, показывающий, успешно ли инициализировали Kafka
    private final int maxRetries; // макс кол-во попыток retry
    private final long retryDelay; // Пауза между retry (мс)
    private final long callbackTimeout; // Таймаут ожидания callback (сек)

    @Autowired
    public KafkaPacketSender(@Qualifier("producerConfigs") Map<String, Object> producerConfigs,
                             TrafficCaptureConfig config) {
        this.config = config;
        this.topicName = config.getKafka().getTopicName() + "." + config.getUser();
        this.kafkaAvailable = false;

        this.maxRetries = config.getKafka().getRetries();
        this.retryDelay = config.getKafka().getRetryDelayMs();
        this.callbackTimeout = config.getKafka().getCallbackTimeoutS();

        // Если режим local => не создаём реальный KafkaProducer
        WorkingMode mode = config.getMode();
        if (mode == WorkingMode.LOCAL) {
            logger.info("KafkaPacketSender: mode=local => skipping KafkaProducer creation.");
            this.kafkaTemplate = null;
            this.kafkaAvailable = false;
        } else {
            try {
                // Принудительно прописываем параметры, чтобы не блокироваться навсегда


                logger.info("KafkaPacketSender: Trying to create KafkaProducer for mode={}", mode);

                ProducerFactory<String, byte[]> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);
                this.kafkaTemplate = new KafkaTemplate<>(producerFactory);

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
        if (kafkaTemplate == null) {
            return false;
        }

        try {
            // создаём одноразовый producer
            ProducerFactory<String, byte[]> pf = kafkaTemplate.getProducerFactory();
            try (var ephemeralProducer = pf.createProducer()) {
                ephemeralProducer.partitionsFor(topicName);
            }
            return true;
        } catch (Exception e) {
            logger.error("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Отправка пакета в Kafka.
     * Асинхронная, но метод всё же "блокируется" на время ожидания callback (до callbackTimeoutSec * maxRetries суммарно).
     * При multiple retry > throw Exception => fallback.
     */
    @Override
    public void sendPacket(org.pcap4j.packet.Packet packet) throws Exception {
        if (kafkaTemplate == null || !kafkaAvailable) {
            throw new IllegalStateException("Kafka not available => fallback");
        }
        // Преобразуем пакет в массив байт
        byte[] rawData = packet.getRawData();

        int countaAttempt = 0;
        AtomicReference<Exception> lastEx = new AtomicReference<>();

        while (countaAttempt < maxRetries) {
            countaAttempt++;
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean successFlag = new AtomicBoolean(false);

            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(topicName, rawData);

            int finalAttempt = countaAttempt;
            future.whenComplete((result, ex) -> {
                try {
                    if (ex == null) {
                        // успех
                        successFlag.set(true);
                        logger.trace("Kafka send SUCCESS => offset={}", result.getRecordMetadata().offset());
                    } else {
                        // ловим ошибку
                        lastEx.set(new Exception(ex));
                        logger.error("Kafka send attempt={} => error: {}", finalAttempt, ex.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });

            // Ждём, пока callback отработает (не более callbackTimeoutSec секунд)
            boolean done = latch.await(callbackTimeout, TimeUnit.SECONDS);
            if (!done) {
                // callback не пришёл => считаем ошибкой
                lastEx.set(new TimeoutException("No callback from Kafka within " + callbackTimeout + "s"));
                logger.warn("Kafka send attempt={} => no callback => treat as fail", countaAttempt);
            }

            if (successFlag.get()) {
                // Успех => выходим
                return;
            } else {
                // ошибка => retry
                Thread.sleep(retryDelay);
            }
        }

        // Дошли сюда => все попытки исчерпаны
        logger.error("All {} attempts to send packet failed => closeProducer => fallback", maxRetries);
        if (lastEx.get() == null) {
            lastEx.set(new IllegalStateException("Kafka send failed (unknown reason)"));
        }
        throw new Exception(lastEx.get());
//        closeProducer();
    }

    /**
     * Закрыть kafkaTemplate и пометить Kafka как недоступную.
     */
    public void closeProducer() {
        kafkaAvailable = false;
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.flush();
                kafkaTemplate.destroy();
                logger.info("Kafka producer closed");
            } catch (Exception e) {
                logger.error("Error closing producer => {}", e.getMessage());
            }
            kafkaTemplate = null;
        }
    }
}
