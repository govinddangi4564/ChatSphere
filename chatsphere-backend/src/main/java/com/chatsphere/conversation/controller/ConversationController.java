package com.chatsphere.conversation.controller;

import com.chatsphere.common.dto.ApiResponse;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.conversation.dto.*;
import com.chatsphere.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Endpoints for private and group conversation management")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/private")
    @Operation(summary = "Create or find private conversation", description = "Creates a new 1-on-1 conversation or returns the existing one (enforcing database uniqueness)")
    public ResponseEntity<ApiResponse<ConversationDetailsResponse>> createOrFindPrivateConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePrivateConversationRequest request
    ) {
        ConversationDetailsResponse response = conversationService.createOrFindPrivateConversation(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "List user conversations", description = "Paginated listing with sort whitelist (lastMessageAt, createdAt, title) and filters (type, archived, muted, unread)")
    public ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "type", required = false) ConversationType type,
            @RequestParam(name = "archived", required = false) Boolean archived,
            @RequestParam(name = "muted", required = false) Boolean muted,
            @RequestParam(name = "unread", required = false) Boolean unread,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        PageResponse<ConversationSummaryResponse> response = conversationService.getConversations(
                userDetails.getUsername(),
                type,
                archived,
                muted,
                unread,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", response));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get conversation details", description = "Retrieves conversation details and participants (requires active membership)")
    public ResponseEntity<ApiResponse<ConversationDetailsResponse>> getConversationDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId
    ) {
        ConversationDetailsResponse response = conversationService.getConversationDetails(userDetails.getUsername(), publicId);
        return ResponseEntity.ok(ApiResponse.success("Conversation details retrieved successfully", response));
    }

    @PutMapping("/{publicId}/archive")
    @Operation(summary = "Archive conversation", description = "Archives or unarchives a conversation for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId,
            @RequestBody(required = false) ArchiveRequest request
    ) {
        boolean archived = request != null ? request.archived() : true;
        conversationService.archiveConversation(userDetails.getUsername(), publicId, archived);
        return ResponseEntity.ok(ApiResponse.success(archived ? "Conversation archived" : "Conversation unarchived", null));
    }

    @PutMapping("/{publicId}/mute")
    @Operation(summary = "Mute conversation", description = "Mutes or unmutes notifications for a conversation")
    public ResponseEntity<ApiResponse<Void>> muteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String publicId,
            @RequestBody(required = false) MuteRequest request
    ) {
        boolean muted = request != null ? request.muted() : true;
        conversationService.muteConversation(userDetails.getUsername(), publicId, muted);
        return ResponseEntity.ok(ApiResponse.success(muted ? "Conversation muted" : "Conversation unmuted", null));
    }
}
