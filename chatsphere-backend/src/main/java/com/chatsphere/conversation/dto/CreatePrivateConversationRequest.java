package com.chatsphere.conversation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePrivateConversationRequest(
        @NotBlank(message = "Recipient public ID is required")
        String recipientPublicId
) {}
