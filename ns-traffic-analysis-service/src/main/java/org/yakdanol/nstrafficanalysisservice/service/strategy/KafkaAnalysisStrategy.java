//package org.yakdanol.nstrafficanalysisservice.service.strategy;
//
//import lombok.extern.slf4j.Slf4j;
//import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
//import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;
//
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@Slf4j
//public class KafkaAnalysisStrategy implements AnalysisStrategy {
//
//    private final TrafficAnalysisConfig config;
//    private final AtomicBoolean running = new AtomicBoolean(false);
//
//    public KafkaAnalysisStrategy(TrafficAnalysisConfig config) {
//        this.config = config;
//    }
//    // какой-нибудь Consumer или KafkaListener
//    // ...
//
//    @Override
//    public void startAnalysis(AnalysisTask task) throws Exception {
//        running.set(true);
//        try {
//            // Инициализируем consumer, подписываемся на topic=task.getKafkaTopic()
//            // poll в цикле (batch), пока running=true
//            // Обработка "пакетов" (пока пустая логика).
//        } catch (Exception e) {
//            log.error("KafkaAnalysisStrategy error: {}", e.getMessage());
//            throw e;
//        } finally {
//            running.set(false);
//            // закрыть consumer
//        }
//    }
//
//    @Override
//    public void stopAnalysis() {
//        running.set(false);
//        // закрыть consumer
//    }
//
//    @Override
//    public boolean isRunning() {
//        return running.get();
//    }
//}
//
