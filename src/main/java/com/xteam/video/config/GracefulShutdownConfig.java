package com.xteam.video.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class GracefulShutdownConfig {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownConfig.class);

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @Value("${graceful-shutdown.wait-seconds:30}")
    private int waitSeconds;

    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Starting graceful shutdown...");
        isShuttingDown.set(true);

        try {
            log.info("Waiting {} seconds for tasks to complete...", waitSeconds);
            Thread.sleep(waitSeconds * 1000L);
            log.info("Graceful shutdown completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Shutdown interrupted");
        }
    }
}