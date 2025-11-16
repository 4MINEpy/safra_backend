package com.safra.safra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Server is running! " + System.currentTimeMillis());
    }

    @PostMapping("/echo")
    public ResponseEntity<String> echo(@RequestBody String message) {
        return ResponseEntity.ok("Echo: " + message);
    }
}