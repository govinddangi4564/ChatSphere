package com.chatsphere.message.service;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.message.dto.CursorApiResponse;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.SendMessageRequest;
import com.chatsphere.message.entity.Message;
import com.chatsphere.message.entity.MessageReceipt;
import com.chatsphere.message.repository.MessageReceiptRepository;
import com.chatsphere.message.repository.MessageRepository;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class MessageService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PREVIEW_LENGTH = 250;

    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;

    public MessageService(
            MessageRepository messageRepository,
            MessageReceiptRepository messageReceiptRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            UserRepository userRepository,
            BlockedUserRepository blockedUserRepository
    ) {
        this.messageRepository = messageRepository;
        this.messageReceiptRepository = messageReceiptRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
    }

    public MessageResponse sendMessage(String currentUsername, SendMessageRequest request) {
        User sender = getUserByUsername(currentUsername);

        Conversation conversation = conversationRepository.findByPublicId(request.conversationPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.conversationPublicId()));

        // Validate membership
        conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.conversationPublicId()));

        List<ConversationMember> members = conversationMemberRepository.findAllByConversationId(conversation.getId());

        // Validate block rules for private conversations
        if (conversation.getConversationType() == ConversationType.PRIVATE) {
            ConversationMember recipientMember = members.stream()
                    .filter(m -> !m.getUser().getId().equals(sender.getId()))
                    .findFirst()
                    .orElse(null);

            if (recipientMember != null && blockedUserRepository.isBidirectionallyBlocked(sender.getId(), recipientMember.getUser().getId())) {
                throw new BadRequestException("Cannot send message: user communication is blocked");
            }
        }

        // Idempotency check: if message with this sender and clientMessageId already exists, return it
        Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(sender.getId(), request.clientMessageId());
        if (existing.isPresent()) {
            return mapToMessageResponse(existing.get());
        }

        // Persist new message
        Message message = new Message();
        message.setClientMessageId(request.clientMessageId());
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(request.messageType() != null ? request.messageType() : MessageType.TEXT);
        message.setContent(request.content().trim());
        message.setCreatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        // Create receipt rows for each recipient and increment unread counts
        for (ConversationMember member : members) {
            if (!member.getUser().getId().equals(sender.getId())) {
                MessageReceipt receipt = new MessageReceipt();
                receipt.setMessage(savedMessage);
                receipt.setRecipient(member.getUser());
                messageReceiptRepository.save(receipt);

                member.setUnreadCount(member.getUnreadCount() + 1);
                conversationMemberRepository.save(member);
            }
        }

        // Update conversation last_message_at and preview
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        String preview = savedMessage.getContent();
        if (preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        return mapToMessageResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public CursorApiResponse<MessageResponse> getMessages(
            String currentUsername,
            String conversationPublicId,
            int size,
            String beforeCursor
    ) {
        User currentUser = getUserByUsername(currentUsername);

        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        // Validate membership
        conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        if (safeSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("Maximum page size is " + MAX_PAGE_SIZE);
        }

        // Fetch size + 1 to check if there are older messages
        PageRequest pageRequest = PageRequest.of(0, safeSize + 1);
        List<Message> queryResults;
        if (beforeCursor == null || beforeCursor.isBlank()) {
            queryResults = messageRepository.findNewestMessages(conversation.getId(), pageRequest);
        } else {
            CursorData cursorData = decodeCursor(beforeCursor);
            queryResults = messageRepository.findMessagesBeforeCursor(
                    conversation.getId(),
                    cursorData.createdAt(),
                    cursorData.id(),
                    pageRequest
            );
        }

        List<Message> messages = new ArrayList<>(queryResults);
        boolean hasMore = messages.size() > safeSize;
        String nextCursor = null;

        if (hasMore) {
            // Because results are in DESC order, the oldest (boundary) message is the last one at index safeSize
            Message boundary = messages.get(safeSize);
            nextCursor = encodeCursor(boundary.getCreatedAt(), boundary.getId());
            // Truncate to safeSize
            messages = new ArrayList<>(messages.subList(0, safeSize));
        }

        // Return messages in chronological (ASC) display order
        Collections.reverse(messages);

        List<MessageResponse> dtos = messages.stream()
                .map(this::mapToMessageResponse)
                .toList();

        return CursorApiResponse.of(dtos, nextCursor, hasMore);
    }

    public MessageResponse mapToMessageResponse(Message message) {
        return new MessageResponse(
                message.getPublicId(),
                message.getClientMessageId(),
                message.getConversation().getPublicId(),
                message.getSender().getPublicId(),
                message.getSender().getUsername(),
                message.getSender().getFullName(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.isEdited(),
                message.isDeletedForEveryone()
        );
    }

    public static String encodeCursor(Instant createdAt, Long id) {
        String raw = createdAt.toString() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorData decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int colonIdx = raw.lastIndexOf(':');
            if (colonIdx <= 0) {
                throw new BadRequestException("Invalid cursor format");
            }
            Instant createdAt = Instant.parse(raw.substring(0, colonIdx));
            Long id = Long.parseLong(raw.substring(colonIdx + 1));
            return new CursorData(createdAt, id);
        } catch (Exception e) {
            throw new BadRequestException("Invalid cursor: " + cursor);
        }
    }

    public record CursorData(Instant createdAt, Long id) {}

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
