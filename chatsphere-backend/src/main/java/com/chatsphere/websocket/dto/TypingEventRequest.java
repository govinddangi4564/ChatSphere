package com.chatsphere.websocket.dto;

import jakarta.validation.constraints.NotBlank;

public record TypingEventRequest(
        @NotBlank(message = "Conversation public ID is required")
        String conversationPublicId,
        boolean typing
) {}
