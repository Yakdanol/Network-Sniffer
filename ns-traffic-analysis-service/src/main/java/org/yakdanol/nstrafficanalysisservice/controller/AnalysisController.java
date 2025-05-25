package org.yakdanol.nstrafficanalysisservice.controller;

import lombok.RequiredArgsConstructor;
import org.pcap4j.core.NotOpenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yakdanol.nstrafficanalysisservice.service.DataSource;
import org.yakdanol.nstrafficanalysisservice.service.processing.ProcessingCoordinatorService;
import org.yakdanol.nstrafficanalysisservice.users.request.AnalysisRequest;
import org.yakdanol.nstrafficanalysisservice.users.storage.Users;
import org.yakdanol.nstrafficanalysisservice.users.storage.UsersService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
class AnalysisController {
    private final UsersService usersService;
    private final ProcessingCoordinatorService processingCoordinatorService;

    /**
     * Запустить live‑анализ пакетов трафика пользователя через Kafka.
     */
    @PostMapping("/live/start/{username}")
    public ResponseEntity<?> startLive(@PathVariable String username) {
        enqueue(username, DataSource.KAFKA);
        String message = String.format("Live-анализ для пользователя '%s' добавлен в очередь.", username);
        return ResponseEntity.accepted().body(message);
    }

    /**
     * Принудительно остановить текущий live‑анализ пользователя через Kafka.
     */
    @PostMapping("/live/stop/{username}")
    public ResponseEntity<?> stopLive(@PathVariable String username) {
        try {
            cancel(username);
            String message = String.format("Live-анализ для пользователя '%s' успешно отменён.", username);
            return ResponseEntity.ok().body(message);
        } catch (NotOpenException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Запустить анализ пакетов трафика пользователя из файла. */
    @PostMapping("/file/start/{username}")
    public ResponseEntity<?> startOffline(@PathVariable String username) {
        enqueue(username, DataSource.FILE);
        String message = String.format("Offline-анализ файла для пользователя '%s' добавлен в очередь.", username);
        return ResponseEntity.accepted().body(message);
    }

    /** Остановить анализ пакетов трафика пользователя из файла. */
    @PostMapping("/file/stop/{username}")
    public ResponseEntity<?> stopOffline(@PathVariable String username) {
        try {
            cancel(username);
            String message = String.format("Offline-анализ файла для пользователя '%s' успешно отменён.", username);
            return ResponseEntity.ok().body(message);
        } catch (NotOpenException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void enqueue(String username, DataSource type) {
        if (!usersService.isUserExist(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown user");
        }
        processingCoordinatorService.enqueue(new AnalysisRequest(username, type));
    }

    private void cancel(String username) throws NotOpenException {
        processingCoordinatorService.cancel(username);
    }

    @GetMapping("/users")
    public List<String> list() {
        return usersService.findAllUsers()
                .stream()
                .map(Users::getFullName)
                .toList();
    }
}
