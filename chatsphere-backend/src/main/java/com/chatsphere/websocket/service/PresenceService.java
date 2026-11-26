package com.chatsphere.websocket.service;

import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final ConcurrentHashMap<String, Integer> userSessionCounts = new ConcurrentHashMap<>();
    private final UserRepository userRepository;

    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void userConnected(String username) {
        if (username == null) return;
        userSessionCounts.compute(username, (k, v) -> (v == null) ? 1 : v + 1);
        log.debug("User {} connected, active sessions: {}", username, userSessionCounts.get(username));
    }

    @Transactional
    public void userDisconnected(String username) {
        if (username == null) return;
        userSessionCounts.computeIfPresent(username, (k, v) -> {
            int newCount = v - 1;
            return newCount > 0 ? newCount : null;
        });

        if (!isUserOnline(username)) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastSeenAt(Instant.now());
                userRepository.save(user);
                log.debug("User {} went offline, updated lastSeenAt", username);
            }
        }
    }

    public boolean isUserOnline(String username) {
        return username != null && userSessionCounts.containsKey(username);
    }
}
