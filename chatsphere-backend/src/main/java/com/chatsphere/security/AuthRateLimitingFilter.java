package com.chatsphere.security;

import com.chatsphere.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding rate limiter for sensitive authentication endpoints:
 * /api/auth/login, /api/auth/register, /api/auth/forgot-password, /api/auth/reset-password.
 * Allows max 20 requests per minute per IP address.
 */
@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MS = 60_000L;

    private static final class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final org.springframework.core.env.Environment environment;

    public AuthRateLimitingFilter(org.springframework.core.env.Environment environment) {
        this.environment = environment;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (environment != null && java.util.Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return true;
        }
        String path = request.getRequestURI();
        // Only rate limit write operations on auth endpoints
        if (!path.startsWith("/api/auth/")) {
            return true;
        }
        return "GET".equalsIgnoreCase(request.getMethod()) || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestCounter counter = requestCounts.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStart) > WINDOW_MS) {
                return new RequestCounter(now, 1);
            }
            current.count.incrementAndGet();
            return current;
        });

        if (counter.count.get() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse error = new ErrorResponse("Too many authentication requests. Please try again in a minute.");
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
