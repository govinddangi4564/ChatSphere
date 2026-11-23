package com.chatsphere.health.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.health.dto.HealthResponse;
import com.chatsphere.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application and database health monitoring endpoints")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(summary = "Check service health", description = "Returns system operational status and database connectivity")
    public ResponseEntity<ApiResponse<HealthResponse>> getHealth() {
        HealthResponse response = healthService.checkHealth();
        return ResponseEntity.ok(ApiResponse.success("ChatSphere API is operational", response));
    }
}
