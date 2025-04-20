//package org.yakdanol.nstrafficanalysisservice.service.concurrency;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.yakdanol.nstrafficanalysisservice.config.TrafficAnalysisConfig;
//import org.yakdanol.nstrafficanalysisservice.model.AnalysisTask;
//
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Slf4j
//@Component
//public class ResourceAllocator {
//
//    private TrafficAnalysisConfig config;
//    private final AtomicInteger usedPoolSize = new AtomicInteger(0);
//    private final int maxPoolSize; // basePoolSize, from config
//
//    public ResourceAllocator(TrafficAnalysisConfig config) {
//        this.config = config;
//        this.maxPoolSize = config.getPoolSize();
//    }
//
//    public boolean allocateForTask(AnalysisTask task) {
//        // можно более умную логику (взять 2 потока, etc.)
//        // Сейчас: если used < max => allocate 1
//        int currentPoolSize = usedPoolSize.get();
//        if (currentPoolSize < maxPoolSize) {
//            usedPoolSize.getAndAdd(2);
//            log.info("Allocated resource for task={}, used={}/{}", task.getId(), usedPoolSize.get(), maxPoolSize);
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public void releaseForTask(AnalysisTask task) {
//        usedPoolSize.decrementAndGet();
//        log.info("Released resource for task={}, used={}/{}", task.getId(), usedPoolSize.get(), maxPoolSize);
//    }
//}
