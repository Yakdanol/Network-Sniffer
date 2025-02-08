package org.yakdanol.nstrafficcaptureservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "traffic-capture")
public class TrafficCaptureConfig {

    private String user;
    private String system;
    private String interfaceName;
    private String logDirectory;
    private String logFormat;
    private String filter;
    private int processingPoolSize;
    private int queueSize;
    private boolean consoleLogging;
}
