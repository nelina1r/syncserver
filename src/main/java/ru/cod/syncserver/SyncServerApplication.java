package ru.cod.syncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

@SpringBootApplication
@RestController
public class SyncServerApplication {
    private final Logger logger = LoggerFactory.getLogger(SyncServerApplication.class);

    // Используем объект-блокировку для синхронизации доступа
    private final Object lock = new Object();
    // Очередь клиентов, ожидающих формирования батча
    private final Queue<String> waitingQueue = new LinkedList<>();
    // Набор клиентов, которым выдано разрешение на обучение (активный батч)
    private final Set<String> currentBatch = new HashSet<>();

    public static void main(String[] args) {
        SpringApplication.run(SyncServerApplication.class, args);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>("PONG", HttpStatus.OK);
    }

    @GetMapping("/acquire")
    public ResponseEntity<String> acquire(@RequestParam String clientId) {
        synchronized (lock) {
            // Если клиент уже в активном батче – возвращаем разрешение
            if (currentBatch.contains(clientId)) {
                logger.info("Client {} is already in the current batch. Permission granted.", clientId);
                return ResponseEntity.ok("Permission granted");
            }

            // Если обучение уже идёт (активный батч не пуст) – регистрируем клиента для следующего батча
            if (!currentBatch.isEmpty()) {
                if (!waitingQueue.contains(clientId)) {
                    waitingQueue.add(clientId);
                    logger.info("Client {} added to waiting queue for next batch. Queue size: {}", clientId, waitingQueue.size());
                } else {
                    logger.info("Client {} is already in waiting queue for next batch. Queue size: {}", clientId, waitingQueue.size());
                }
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Training in progress, waiting for next batch");
            }

            // Если обучение не идёт, добавляем клиента в очередь ожидания (если его там ещё нет)
            if (!waitingQueue.contains(clientId)) {
                waitingQueue.add(clientId);
            }
            logger.info("Client {} requested permission. Waiting queue size: {}", clientId, waitingQueue.size());

            // Если в очереди накопилось 3 и более клиента, формируем новый батч из первых 3 клиентов
            if (waitingQueue.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    String c = waitingQueue.poll();
                    currentBatch.add(c);
                }
                logger.info("New batch started with clients: {}", currentBatch);
            }

            // Если клиент входит в сформированный батч, выдаём разрешение, иначе – информируем о необходимости ждать
            if (currentBatch.contains(clientId)) {
                return ResponseEntity.ok("Permission granted");
            } else {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Waiting for another client");
            }
        }
    }

    @PostMapping("/release")
    public ResponseEntity<String> release(@RequestParam String clientId) {
        synchronized (lock) {
            if (currentBatch.remove(clientId)) {
                logger.info("Client {} released permission. Current batch size: {}", clientId, currentBatch.size());
                if (currentBatch.isEmpty()) {
                    logger.info("Batch completed. Ready for next batch.");
                }
                return ResponseEntity.ok("Permission released");
            } else {
                logger.warn("Client {} attempted to release permission but was not in the current batch.", clientId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Client was not in training batch.");
            }
        }
    }
}
