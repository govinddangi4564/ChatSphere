package com.chatsphere.health.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        Instant timestamp,
        String database,
        String version
) {}
