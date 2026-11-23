package com.chatsphere.health.service;

import com.chatsphere.health.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthResponse checkHealth() {
        String dbStatus = evaluateDatabaseStatus();
        String appStatus = "UP".equals(dbStatus) ? "UP" : "DEGRADED";
        return new HealthResponse(appStatus, Instant.now(), dbStatus, "1.0.0");
    }

    private String evaluateDatabaseStatus() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return "UP";
        } catch (Exception ex) {
            log.warn("Database connectivity check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}
