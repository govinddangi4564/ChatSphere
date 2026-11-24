package com.chatsphere.user.dto;

import java.time.Instant;

public record BlockedUserResponse(
        String publicId,
        String fullName,
        String username,
        String profileImageUrl,
        Instant blockedAt
) {}
