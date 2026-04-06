package com.contied.global.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String healthCheck() {
        return "OK";
    }

    @GetMapping("/api/v1/health")
    public String healthCheckV1() {
        return "OK";
    }
}
