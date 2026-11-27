package com.chatsphere.message.dto;

import jakarta.validation.constraints.NotBlank;

public record ForwardMessageRequest(
        @NotBlank(message = "Target conversation public ID is required")
        String targetConversationPublicId
) {}
