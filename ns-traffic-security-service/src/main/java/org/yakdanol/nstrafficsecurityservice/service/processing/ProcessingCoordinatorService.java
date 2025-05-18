package org.yakdanol.nstrafficsecurityservice.service.processing;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapNativeException;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficsecurityservice.config.TrafficSecurityConfig;
import org.yakdanol.nstrafficsecurityservice.service.consumer.*;
import org.yakdanol.nstrafficsecurityservice.service.security.TrafficSecurityService;
import org.yakdanol.nstrafficsecurityservice.users.request.SecurityAnalysisRequest;
import org.yakdanol.nstrafficsecurityservice.storage.users.Users;
import org.yakdanol.nstrafficsecurityservice.storage.users.UsersRepository;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessingCoordinatorService {

    private final TrafficSecurityConfig configs;
    private final KafkaPacketConsumer kafkaConsumer;
    private final FilePacketConsumer fileConsumer;
    private final TrafficSecurityService securityService;
    private final UsersRepository repository;

    /** Очередь входящих задач. */
    private BlockingQueue<SecurityAnalysisRequest> queue;
    /** Один воркер‑поток. */
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    /** Текущий consumer — нужен, чтобы послать cancel(). */
    private volatile PacketConsumer activeConsumer;
    /** Текущий пользователь. */
    private volatile String currentUser;

    @PostConstruct
    void init() {
        queue = new ArrayBlockingQueue<>(configs.getGeneralConfigs().getQueueSize());
        worker.submit(this::loop);
        log.info("ProcessingCoordinatorService started, queue capacity = {}", configs.getGeneralConfigs().getQueueSize());
    }

    @PreDestroy
    void shutdown() {
        worker.shutdownNow();
        log.info("ProcessingCoordinatorService stopped");
    }

    /**
     * Кладёт задачу в очередь.
     */
    public void enqueue(SecurityAnalysisRequest task) {
        boolean ok = queue.offer(task);
        log.info("Enqueue task [{}|{}] -> {}", task.getFullUserName(), task.getType(), ok ? "OK" : "QUEUE FULL");
    }

    /** Отмена анализа пользователю. */
    public void cancel(String fullName) throws NotOpenException {
        // 1) Попробовать удалить из очереди
        boolean removed = queue.removeIf(t -> t.getFullUserName().equals(fullName));
        if (removed) log.debug("Task of [{}] removed from queue", fullName);

        // 2) Если уже обрабатывается — остановить обработку
        if (fullName.equals(currentUser) && activeConsumer != null) {
            activeConsumer.cancel();
            log.info("Active task of [{}] cancelled", fullName);
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void loop() {
        try {
            while (true) {
                SecurityAnalysisRequest task = queue.take();
                currentUser = task.getFullUserName();
                log.info("Start processing {} in mode {}", currentUser, task.getType());

                try {
                    switch (task.getType()) {
                        case KAFKA -> processKafka(task);
                        case FILE -> processFile(task);
                    }
                    log.info("Finished processing {} ({})", currentUser, task.getType());
                } catch (CancellationException e) {
                    log.info("Processing of {} ({}) was cancelled", currentUser, task.getType());
                } catch (Exception e) {
                    log.warn("Processing of {} failed: {}", currentUser, e.getMessage(), e);
                } finally {
                    currentUser = null;
                    activeConsumer = null;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.info("Worker loop interrupted, exiting");
        }
    }

    private void processKafka(SecurityAnalysisRequest task) {
        Users user = repository.findByFullName(task.getFullUserName()).orElseThrow();
        kafkaConsumer.subscribe(user.getKafkaTopicName());
        activeConsumer = kafkaConsumer;
        try {
            securityService.analyse(kafkaConsumer, user.getInternalUserName(), user.getFullName());
        } catch (RuntimeException ex) {
            if (!(ex instanceof CancellationException)) throw ex;
        } finally {
            kafkaConsumer.unsubscribe();
        }
    }

    private void processFile(SecurityAnalysisRequest task) throws URISyntaxException {
        Users user = repository.findByFullName(task.getFullUserName()).orElseThrow();
        URL path = getClass().getClassLoader().getResource(configs.getFileConfigs().getDirectory() + user.getInternalUserName() + ".pcap");
        File pcap = new File(path.toURI()).toPath().toFile();
        if (!pcap.exists()) {
            log.debug("pcap file {} not found for {}", pcap, task.getFullUserName());
            return;
        }
        try {
            fileConsumer.open(pcap);
            activeConsumer = fileConsumer;
            securityService.analyse(fileConsumer, user.getInternalUserName(), user.getFullName());
        } catch (RuntimeException ex) {
            if (!(ex instanceof CancellationException)) throw ex;
        } catch (PcapNativeException e) {
            throw new RuntimeException(e);
        }
    }
}
