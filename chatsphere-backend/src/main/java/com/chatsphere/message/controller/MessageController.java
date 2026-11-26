package com.chatsphere.message.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.message.dto.CursorApiResponse;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Messages", description = "Message sending and retrieval endpoints")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a new message with idempotency")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        MessageResponse response = messageService.sendMessage(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", response));
    }

    @GetMapping("/conversations/{publicId}/messages")
    @Operation(summary = "Get conversation messages with keyset cursor pagination")
    public ResponseEntity<CursorApiResponse<MessageResponse>> getMessages(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String before,
            Authentication authentication
    ) {
        CursorApiResponse<MessageResponse> response = messageService.getMessages(
                authentication.getName(),
                publicId,
                size,
                before
        );
        return ResponseEntity.ok(response);
    }
}
