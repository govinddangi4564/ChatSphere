package com.chatsphere.conversation.dto;

import java.time.Instant;

public record ConversationParticipantResponse(
        String publicId,
        String fullName,
        String username,
        String profileImageUrl,
        Instant lastSeenAt,
        String role
) {}
