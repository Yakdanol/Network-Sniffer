package org.yakdanol.nstrafficcaptureservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrafficCaptureController {

    @GetMapping("/health")
    public String healthCheck() {
        return "Traffic Capture Service is running.";
    }
}
