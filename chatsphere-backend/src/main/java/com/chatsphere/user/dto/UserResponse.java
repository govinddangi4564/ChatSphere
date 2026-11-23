package com.chatsphere.user.dto;

import java.time.Instant;

public record UserResponse(
        String publicId,
        String fullName,
        String username,
        String email,
        String profileImageUrl,
        String bio,
        String accountStatus,
        boolean emailVerified,
        Instant createdAt
) {}
