package org.yakdanol.nstrafficanalysisservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;
import org.yakdanol.nstrafficanalysisservice.service.strategy.AnalysisStrategy;
import org.yakdanol.nstrafficanalysisservice.service.strategy.FileAnalysisStrategy;
import org.yakdanol.nstrafficanalysisservice.service.strategy.KafkaAnalysisStrategy;

@Slf4j
@Service
public class AnalysisService {

    private final TrafficAnalysisConfig config;
    private AnalysisStrategy currentStrategy;
    private boolean running;

    public AnalysisService(TrafficAnalysisConfig config) {
        this.config = config;
    }

    // Запускаем анализ одного задания
    public void startAnalysis(AnalysisTask task) throws Exception {
        if (running) {
            throw new IllegalStateException("AnalysisService is busy with another task");
        }
        this.running = true;
        this.currentStrategy = createStrategyFor(task);
        try {
            currentStrategy.startAnalysis(task);
        } finally {
            this.running = false;
        }
    }

    public void stopAnalysis() {
        if (currentStrategy != null && currentStrategy.isRunning()) {
            currentStrategy.stopAnalysis();
        }
        this.running = false;
    }

    private AnalysisStrategy createStrategyFor(AnalysisTask task) {
        return switch (task.getTypeTask().toUpperCase()) {
            case "FILE"  -> new FileAnalysisStrategy(config);
            case "KAFKA" -> new KafkaAnalysisStrategy(config);
            default -> throw new IllegalArgumentException("Unknown type: " + task.getTypeTask());
        };
    }
}
