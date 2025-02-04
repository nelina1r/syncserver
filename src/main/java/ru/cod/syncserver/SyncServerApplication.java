package ru.cod.syncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@RestController
public class SyncServerApplication {

    private final AtomicInteger activeClients = new AtomicInteger(0);

    public static void main(String[] args) {
        SpringApplication.run(SyncServerApplication.class, args);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping () {
        return new ResponseEntity<>("PONG", HttpStatus.OK);
    }

    @GetMapping("/acquire")
    public ResponseEntity<String> acquire() {
        if (activeClients.get() < 2) {
            activeClients.incrementAndGet();
            return ResponseEntity.ok("Permission granted");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many clients");
        }
    }

    @PostMapping("/release")
    public ResponseEntity<String> release() {
        activeClients.decrementAndGet();
        return ResponseEntity.ok("Permission released");
    }
}