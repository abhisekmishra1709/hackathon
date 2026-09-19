package com.sentinel.aml.system;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "service", "Sentinel AML",
                "status", "UP",
                "timestamp", Instant.now());
    }
}