package com.chatsphere.message.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.message.dto.*;
import com.chatsphere.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Messages", description = "Message management endpoints (send, history, reply, edit, delete, forward, reactions, pins, search)")
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

    @PutMapping("/messages/{publicId}")
    @Operation(summary = "Edit own message", description = "Updates text of sender's message and marks it edited")
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable String publicId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication authentication
    ) {
        MessageResponse response = messageService.editMessage(authentication.getName(), publicId, request);
        return ResponseEntity.ok(ApiResponse.success("Message updated successfully", response));
    }

    @DeleteMapping("/messages/{publicId}")
    @Operation(summary = "Delete message", description = "Deletes message for current user, or for everyone if forEveryone=true within time limit")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "false") boolean forEveryone,
            Authentication authentication
    ) {
        messageService.deleteMessage(authentication.getName(), publicId, forEveryone);
        return ResponseEntity.ok(ApiResponse.success(
                forEveryone ? "Message deleted for everyone" : "Message deleted for you",
                null
        ));
    }

    @PostMapping("/messages/{publicId}/forward")
    @Operation(summary = "Forward message", description = "Forwards message to target conversation if authorized")
    public ResponseEntity<ApiResponse<MessageResponse>> forwardMessage(
            @PathVariable String publicId,
            @Valid @RequestBody ForwardMessageRequest request,
            Authentication authentication
    ) {
        MessageResponse response = messageService.forwardMessage(authentication.getName(), publicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message forwarded successfully", response));
    }

    @PostMapping("/messages/{publicId}/reactions")
    @Operation(summary = "Add or update message reaction")
    public ResponseEntity<ApiResponse<MessageResponse>> addReaction(
            @PathVariable String publicId,
            @Valid @RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        MessageResponse response = messageService.addReaction(authentication.getName(), publicId, request);
        return ResponseEntity.ok(ApiResponse.success("Reaction updated", response));
    }

    @DeleteMapping("/messages/{publicId}/reactions")
    @Operation(summary = "Remove reaction from message")
    public ResponseEntity<ApiResponse<MessageResponse>> removeReaction(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        MessageResponse response = messageService.removeReaction(authentication.getName(), publicId);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed", response));
    }

    @PostMapping("/messages/{publicId}/pin")
    @Operation(summary = "Pin message in conversation")
    public ResponseEntity<ApiResponse<MessageResponse>> pinMessage(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        MessageResponse response = messageService.pinMessage(authentication.getName(), publicId, true);
        return ResponseEntity.ok(ApiResponse.success("Message pinned", response));
    }

    @DeleteMapping("/messages/{publicId}/pin")
    @Operation(summary = "Unpin message in conversation")
    public ResponseEntity<ApiResponse<MessageResponse>> unpinMessage(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        MessageResponse response = messageService.pinMessage(authentication.getName(), publicId, false);
        return ResponseEntity.ok(ApiResponse.success("Message unpinned", response));
    }

    @GetMapping("/conversations/{publicId}/pinned")
    @Operation(summary = "Get pinned messages for conversation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPinnedMessages(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        List<MessageResponse> response = messageService.getPinnedMessages(authentication.getName(), publicId);
        return ResponseEntity.ok(ApiResponse.success("Pinned messages retrieved", response));
    }

    @GetMapping("/messages/search")
    @Operation(summary = "Search messages across accessible conversations")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> searchMessages(
            @RequestParam(required = false) String conversationId,
            @RequestParam String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        PageResponse<MessageResponse> response = messageService.searchMessages(
                authentication.getName(),
                conversationId,
                query,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", response));
    }
}
