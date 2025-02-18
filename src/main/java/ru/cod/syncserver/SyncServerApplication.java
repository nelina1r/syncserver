package ru.cod.syncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@RestController
public class SyncServerApplication {
    Logger logger = LoggerFactory.getLogger(SyncServerApplication.class);

    /**
     * Список клиентов, которые запросили разрешение
     */
    private final ConcurrentHashMap<String, Boolean> idToWaitingStatusClientMap = new ConcurrentHashMap<>();
    /**
     * Кол-во активных клиентов
     */
    private final AtomicInteger activeClients = new AtomicInteger(0);

    public static void main(String[] args) {
        SpringApplication.run(SyncServerApplication.class, args);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>("PONG", HttpStatus.OK);
    }

    @GetMapping("/acquire")
    public ResponseEntity<String> acquire(@RequestParam String clientId) {
        // Добавляем клиента в список ожидания
        idToWaitingStatusClientMap.put(clientId, true);
        logger.info("Client {} requested permission. Waiting clients: {}", clientId, idToWaitingStatusClientMap.size());

        // Если в списке ожидания три клиента, разрешаем обучение
        if (idToWaitingStatusClientMap.size() >= 3) {
            activeClients.set(2); // Устанавливаем счетчик активных клиентов в 2
            idToWaitingStatusClientMap.clear(); // Очищаем список ожидания
            logger.info("Permission granted to two clients.");
            return ResponseEntity.ok("Permission granted");
        } else {
            // Если клиент только один, он должен ждать
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Waiting for another client");
        }
    }

    @PostMapping("/release")
    public ResponseEntity<String> release(@RequestParam String clientId) {
        activeClients.decrementAndGet();
        logger.info("Client {} released permission. Active clients: {}", clientId, activeClients.get());
        return ResponseEntity.ok("Permission released");
    }
}