package com.chatsphere.user.dto;

import java.time.Instant;

public record UserPublicProfileResponse(
        String publicId,
        String fullName,
        String username,
        String profileImageUrl,
        String bio,
        Instant lastSeenAt,
        boolean isContact,
        boolean isBlocked
) {}
