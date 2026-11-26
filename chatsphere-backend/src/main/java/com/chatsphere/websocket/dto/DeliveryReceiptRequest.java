package com.chatsphere.websocket.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryReceiptRequest(
        @NotBlank(message = "Message public ID is required")
        String messagePublicId
) {}
