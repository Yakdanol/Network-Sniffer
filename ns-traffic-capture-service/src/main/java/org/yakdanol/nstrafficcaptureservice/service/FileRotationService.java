package org.yakdanol.nstrafficcaptureservice.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.yakdanol.nstrafficcaptureservice.repository.CapturedPacketRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@Slf4j
@Service
public class FileRotationService {
    private static final Logger logger = LoggerFactory.getLogger(FileRotationService.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CapturedPacketRepository repository;
    private ScheduledFuture<?> rotationTask;

    public FileRotationService(CapturedPacketRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void start() {
        long initialDelay = computeInitialDelay();
        long period = Duration.ofDays(1).toMillis();

        rotationTask = scheduler.scheduleAtFixedRate(this::rotateFile, initialDelay, period, TimeUnit.MILLISECONDS);
        logger.info("File rotation scheduled to start in {} milliseconds and repeat every {} milliseconds", initialDelay, period);
    }

    private long computeInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return Duration.between(now, nextRun).toMillis();
    }

    private void rotateFile() {
        try {
            repository.rotateLogFile();
            logger.info("Rotated logger file at {}", LocalDate.now());
        } catch (Exception e) {
            logger.error("Error during logger file rotation", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel(true);
        }
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during scheduler shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
}
