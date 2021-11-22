package com.michaelpippolito.utils.health;

import com.michaelpippolito.utils.server.ServerStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ServerStatus> getHealth() {
        return ResponseEntity.ok(ServerStatus.UP);
    }
}
