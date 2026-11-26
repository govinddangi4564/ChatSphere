package com.chatsphere.websocket.dto;

public record TypingEventResponse(
        String conversationPublicId,
        String senderPublicId,
        String senderUsername,
        boolean typing
) {}
