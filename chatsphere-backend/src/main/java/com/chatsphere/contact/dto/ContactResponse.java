package com.chatsphere.contact.dto;

import java.time.Instant;

public record ContactResponse(
        String publicId,
        String fullName,
        String username,
        String profileImageUrl,
        String bio,
        Instant lastSeenAt,
        Instant addedAt
) {}
