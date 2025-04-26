package org.yakdanol.nstrafficanalysisservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
@Service
public class AnalysisOrchestrator {

    private final TrafficAnalysisConfig config;
    private final AnalysisService analysisService; // Сервис, который реально обрабатывает 1 задание

    private final Queue<AnalysisTask> taskQueue = new LinkedList<>();
    private boolean analyzing = false; // флаг, идёт ли анализ

    public AnalysisOrchestrator(TrafficAnalysisConfig config, AnalysisService analysisService) {
        this.config = config;
        this.analysisService = analysisService;
    }

    public synchronized void addTasks(List<AnalysisTask> tasks) {
        tasks.forEach(t -> {
            t.setStatus("QUEUED");
            taskQueue.add(t);
        });
    }

    public synchronized void startAll() {
//        if ("parallel".equalsIgnoreCase(config.getProcessingMode())) {
//            startInParallel();
//        } else {
//            startSequential();
//        }

        startSequential();
    }

    private void startSequential() {
        if (analyzing) return;
        analyzing = true;

        new Thread(() -> {
            while (!taskQueue.isEmpty()) {
                AnalysisTask task = taskQueue.poll();
                try {
                    task.setStatus("RUNNING");
                    analysisService.startAnalysis(task);
                    task.setStatus("DONE");
                } catch (Exception e) {
                    // при ошибке обработки просто пропускаем
                    log.error("Analysis error for task: {}", task.getId(), e);
                    task.setStatus("ERROR");
                    task.setErrorMessage(e.getMessage());
                }
            }
            analyzing = false;
        }, "Analysis-Sequential-Thread").start();
    }

    public synchronized void stopAll() {
        // При sequential - останавливаем текущий
        // При parallel - останавливаем все
        analysisService.stopAnalysis();
    }
}
