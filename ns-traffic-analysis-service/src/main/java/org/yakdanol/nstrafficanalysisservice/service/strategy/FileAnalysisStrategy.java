package org.yakdanol.nstrafficanalysisservice.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class FileAnalysisStrategy implements AnalysisStrategy {

    private final TrafficAnalysisConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private FileBatchReader reader;

    public FileAnalysisStrategy(TrafficAnalysisConfig config) {
        this.config = config;
    }

    @Override
    public void startAnalysis(AnalysisTask task) throws Exception {
        running.set(true);
        try {
            // Открываем FileBatchReader (буферизированное чтение)
            this.reader = new FileBatchReader(config);
            while (running.get()) {
                // считываем batch
                var batch = reader.readNextBatch(10); // к примеру 10 строк
                if (batch == null || batch.isEmpty()) {
                    break; // конец файла
                }
                // обрабатываем batch (пока "пустая логика")
                // task.setProcessedPackets(...) и т.д.
            }
        } catch (Exception e) {
            log.error("FileAnalysisStrategy error: {}", e.getMessage());
            throw e;
        } finally {
            running.set(false);
//            if (reader != null) {
//                reader.close();
//            }
        }
    }

    @Override
    public void stopAnalysis() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}

