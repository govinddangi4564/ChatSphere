package com.chatsphere.message.dto;

import java.time.Instant;

public record ReactionResponse(
        String emoji,
        String userPublicId,
        String username,
        Instant createdAt
) {}
