package com.chatsphere.websocket.service;

import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final Duration presenceTtl;
    private final Duration typingTtl;

    // Local fallback in memory in case Redis is unreachable
    private final ConcurrentHashMap<String, Integer> localSessionCounts = new ConcurrentHashMap<>();

    public PresenceService(
            UserRepository userRepository,
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.cache.presence-ttl-seconds:60}") long presenceTtlSeconds,
            @Value("${app.cache.typing-ttl-seconds:5}") long typingTtlSeconds
    ) {
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.presenceTtl = Duration.ofSeconds(presenceTtlSeconds);
        this.typingTtl = Duration.ofSeconds(typingTtlSeconds);
    }

    public void userConnected(String username) {
        if (username == null) return;
        localSessionCounts.compute(username, (k, v) -> (v == null) ? 1 : v + 1);

        try {
            String key = "presence:user:" + username;
            stringRedisTemplate.opsForValue().set(key, "ONLINE", presenceTtl);
            log.debug("Redis presence SET: {} with TTL {}s", key, presenceTtl.toSeconds());
        } catch (Exception e) {
            log.warn("Redis presence update failed; falling back to local memory: {}", e.getMessage());
        }
    }

    @Transactional
    public void userDisconnected(String username) {
        if (username == null) return;
        localSessionCounts.computeIfPresent(username, (k, v) -> {
            int newCount = v - 1;
            return newCount > 0 ? newCount : null;
        });

        if (!isUserOnline(username)) {
            try {
                stringRedisTemplate.delete("presence:user:" + username);
            } catch (Exception e) {
                log.warn("Redis presence delete failed: {}", e.getMessage());
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastSeenAt(Instant.now());
                userRepository.save(user);
                log.debug("User {} went offline, updated lastSeenAt in database", username);
            }
        }
    }

    public boolean isUserOnline(String username) {
        if (username == null) return false;

        try {
            String val = stringRedisTemplate.opsForValue().get("presence:user:" + username);
            if (val != null) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis presence query failed, using local fallback: {}", e.getMessage());
        }

        return localSessionCounts.containsKey(username);
    }

    public void setTyping(String conversationPublicId, String username, boolean typing) {
        if (conversationPublicId == null || username == null) return;
        String key = "typing:conv:" + conversationPublicId + ":user:" + username;
        try {
            if (typing) {
                stringRedisTemplate.opsForValue().set(key, "TYPING", typingTtl);
            } else {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Redis typing state update failed: {}", e.getMessage());
        }
    }

    public boolean isUserTyping(String conversationPublicId, String username) {
        if (conversationPublicId == null || username == null) return false;
        try {
            return stringRedisTemplate.hasKey("typing:conv:" + conversationPublicId + ":user:" + username);
        } catch (Exception e) {
            log.warn("Redis typing check failed: {}", e.getMessage());
            return false;
        }
    }
}
