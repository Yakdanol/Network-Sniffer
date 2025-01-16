package org.yakdanol.nstrafficcaptureservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@Slf4j
@Service
public class FileRotationService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> rotationTask;

    @PostConstruct
    public void start() {
        long initialDelay = computeInitialDelay();
        long period = Duration.ofDays(1).toMillis();

        rotationTask = scheduler.scheduleAtFixedRate(this::rotateFile, initialDelay, period, TimeUnit.MILLISECONDS);
        log.info("File rotation scheduled to start in {} milliseconds and repeat every {} milliseconds", initialDelay, period);
    }

    private long computeInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return Duration.between(now, nextRun).toMillis();
    }

    private void rotateFile() {
        // Реализация смены файла логирования
        log.info("Rotating log file at {}", LocalDate.now());
        // Логика ротации файлов, например, закрытие текущего файла и открытие нового
        // Это может потребовать взаимодействия с CapturedPacketRepository
    }

    @PreDestroy
    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel(true);
        }
        scheduler.shutdownNow();
    }
}
