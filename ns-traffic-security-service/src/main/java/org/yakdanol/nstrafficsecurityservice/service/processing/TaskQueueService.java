//package org.yakdanol.nstrafficsecurityservice.service.processing;
//
//import jakarta.annotation.PreDestroy;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.context.event.EventListener;
//import org.springframework.http.HttpStatus;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//import org.yakdanol.nstrafficsecurityservice.config.TrafficSecurityConfig;
//import org.yakdanol.nstrafficsecurityservice.users.request.TaskCanceller;
//import org.yakdanol.nstrafficsecurityservice.users.request.SecurityAnalysisRequest;
//
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.*;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class TaskQueueService {
//    private final ApplicationEventPublisher events;
//
//    /** One global worker → ordered execution as required. */
//    private final ExecutorService worker = Executors.newSingleThreadExecutor();
//    private final BlockingQueue<SecurityAnalysisRequest> queue = new LinkedBlockingQueue<>();
//    private final Map<String, TaskCanceller> inFlight = new ConcurrentHashMap<>();
//
//    /**
//     * Кладём задачу в очередь. Выбрасываем, если уже есть активная или ожидающая.
//     */
//    public void enqueue(SecurityAnalysisRequest req) {
//        if (inFlight.containsKey(req.username()) || queue.stream().anyMatch(r -> r.username().equals(req.username()))) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "analysis already scheduled or running");
//        }
//        queue.add(req);
//        events.publishEvent(req);
//    }
//
//    /** Отмена активной или ожидающей задачи. */
//    public void cancel(String username) {
//        queue.removeIf(r -> r.username().equals(username));
//        Optional.ofNullable(inFlight.get(username)).ifPresent(TaskCanceller::cancel);
//    }
//
//    /**
//     * Асинхронный слушатель запускает worker, как только пришла первая задача.
//     */
//    @EventListener
//    @Async
//    void onNewTask(SecurityAnalysisRequest ignored) {
//        worker.submit(this::loop);
//    }
//
//    private void loop() {
//        try {
//            while (true) {
//                SecurityAnalysisRequest task = queue.poll(500, TimeUnit.MILLISECONDS);
//                if (task == null) return; // finish when queue is empty for some time
//                inFlight.put(task.username(), new TaskCanceller());
//                try {
//                    events.publishEvent(new InternalStartEvent(task));
//                } finally {
//                    inFlight.remove(task.username());
//                }
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    @PreDestroy
//    void shutdown() {
//        worker.shutdownNow();
//    }
//
//    record InternalStartEvent(SecurityAnalysisRequest req) {}
//}
