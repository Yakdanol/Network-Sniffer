package org.yakdanol.nstrafficanalysisservice.service.strategy;

import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;

public interface AnalysisStrategy {

    // Запускает анализ пакетов для указанного источника
    void startAnalysis(AnalysisTask task) throws Exception;

    // Завершение анализа файла
    void stopAnalysis();

    // Статус работы (true -> RUNNING)
    boolean isRunning();
}
