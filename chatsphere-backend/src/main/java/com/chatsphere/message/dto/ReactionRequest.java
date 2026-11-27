package com.chatsphere.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
        @NotBlank(message = "Emoji is required")
        @Size(max = 32, message = "Emoji cannot exceed 32 characters")
        String emoji
) {}
