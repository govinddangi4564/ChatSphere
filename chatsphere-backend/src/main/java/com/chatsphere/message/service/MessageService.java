package com.chatsphere.message.service;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.attachment.entity.MessageAttachment;
import com.chatsphere.attachment.repository.MessageAttachmentRepository;
import com.chatsphere.attachment.service.AttachmentService;
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
import com.chatsphere.websocket.dto.ReceiptNotification;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final AttachmentService attachmentService;
    private final MessageAttachmentRepository messageAttachmentRepository;

    public MessageService(
            MessageRepository messageRepository,
            MessageReceiptRepository messageReceiptRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            UserRepository userRepository,
            BlockedUserRepository blockedUserRepository,
            SimpMessagingTemplate messagingTemplate,
            AttachmentService attachmentService,
            MessageAttachmentRepository messageAttachmentRepository
    ) {
        this.messageRepository = messageRepository;
        this.messageReceiptRepository = messageReceiptRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.messagingTemplate = messagingTemplate;
        this.attachmentService = attachmentService;
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    public MessageResponse sendMessage(String currentUsername, SendMessageRequest request) {
        User sender = getUserByUsername(currentUsername);

        Conversation conversation = conversationRepository.findByPublicId(request.conversationPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.conversationPublicId()));

        // Validate active membership
        ConversationMember senderMember = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.conversationPublicId()));

        if (senderMember.getLeftAt() != null) {
            throw new AccessDeniedException("User is not an active member of this conversation");
        }

        List<ConversationMember> allMembers = conversationMemberRepository.findAllByConversationId(conversation.getId());
        List<ConversationMember> activeMembers = allMembers.stream()
                .filter(m -> m.getLeftAt() == null)
                .toList();

        // Validate block rules for private conversations
        if (conversation.getConversationType() == ConversationType.PRIVATE) {
            ConversationMember recipientMember = activeMembers.stream()
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

        // Content or attachment validation
        boolean hasContent = request.content() != null && !request.content().trim().isEmpty();
        boolean hasAttachments = request.attachmentPublicIds() != null && !request.attachmentPublicIds().isEmpty();
        if (!hasContent && !hasAttachments) {
            throw new BadRequestException("Message must have either text content or an attachment");
        }

        // Persist new message
        Message message = new Message();
        message.setClientMessageId(request.clientMessageId());
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(request.messageType() != null ? request.messageType() : (hasAttachments ? MessageType.IMAGE : MessageType.TEXT));
        message.setContent(hasContent ? request.content().trim() : "");
        message.setCreatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        // Link attachments
        if (hasAttachments) {
            attachmentService.linkAttachmentsToMessage(savedMessage, request.attachmentPublicIds());
        }

        // Create receipt rows for each active recipient and increment unread counts
        for (ConversationMember member : activeMembers) {
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
        if (preview == null || preview.isEmpty()) {
            preview = "[" + savedMessage.getMessageType().name() + "]";
        } else if (preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        return mapToMessageResponse(savedMessage);
    }

    public MessageResponse sendSystemMessage(Conversation conversation, String content) {
        Message message = new Message();
        message.setClientMessageId("sys-" + UUID.randomUUID());
        message.setConversation(conversation);
        message.setSender(conversation.getCreatedBy());
        message.setMessageType(MessageType.SYSTEM);
        message.setContent(content.trim());
        message.setCreatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        String preview = savedMessage.getContent();
        if (preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        MessageResponse response = mapToMessageResponse(savedMessage);

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversation.getPublicId(),
                response
        );

        return response;
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

        // Validate active membership
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        if (member.getLeftAt() != null) {
            throw new AccessDeniedException("User is not an active member of this conversation");
        }

        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("Maximum page size is " + MAX_PAGE_SIZE);
        }

        // Fetch size + 1 to detect whether hasMore is true
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<Message> messages;

        if (beforeCursor != null && !beforeCursor.isBlank()) {
            CursorData cursorData = decodeCursor(beforeCursor);
            messages = new ArrayList<>(messageRepository.findMessagesBeforeCursor(
                    conversation.getId(),
                    cursorData.createdAt(),
                    cursorData.id(),
                    pageRequest
            ));
        } else {
            messages = new ArrayList<>(messageRepository.findNewestMessages(
                    conversation.getId(),
                    pageRequest
            ));
        }

        boolean hasMore = messages.size() > pageSize;
        String nextCursor = null;

        if (hasMore) {
            Message boundaryMessage = messages.get(pageSize);
            nextCursor = encodeCursor(boundaryMessage.getCreatedAt(), boundaryMessage.getId());
            messages.remove(pageSize);
        }

        // Return messages in chronological (ASC) display order
        Collections.reverse(messages);

        List<MessageResponse> dtos = messages.stream()
                .map(this::mapToMessageResponse)
                .toList();

        return CursorApiResponse.of(dtos, nextCursor, hasMore);
    }

    public MessageResponse mapToMessageResponse(Message message) {
        List<AttachmentResponse> attachments = List.of();
        if (message.getId() != null) {
            List<MessageAttachment> attEntities = messageAttachmentRepository.findAllByMessageId(message.getId());
            if (attEntities != null && !attEntities.isEmpty()) {
                attachments = attEntities.stream()
                        .map(attachmentService::toResponse)
                        .toList();
            }
        }

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
                message.isDeletedForEveryone(),
                attachments
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

    public ReceiptNotification markMessageDelivered(String currentUsername, String messagePublicId) {
        User recipient = getUserByUsername(currentUsername);

        MessageReceipt receipt = messageReceiptRepository.findByMessagePublicIdAndRecipientId(messagePublicId, recipient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MessageReceipt", "messagePublicId", messagePublicId));

        if (receipt.getDeliveredAt() == null) {
            receipt.setDeliveredAt(Instant.now());
            messageReceiptRepository.save(receipt);
        }

        return new ReceiptNotification(
                receipt.getMessage().getPublicId(),
                receipt.getMessage().getConversation().getPublicId(),
                recipient.getPublicId(),
                "DELIVERED",
                receipt.getDeliveredAt()
        );
    }

    public List<ReceiptNotification> markConversationRead(String currentUsername, String conversationPublicId) {
        User recipient = getUserByUsername(currentUsername);

        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), recipient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        member.setUnreadCount(0);
        conversationMemberRepository.save(member);

        List<MessageReceipt> unreadReceipts = messageReceiptRepository.findUnreadReceiptsForConversation(
                conversation.getId(),
                recipient.getId()
        );

        Instant now = Instant.now();
        List<ReceiptNotification> notifications = new ArrayList<>();

        for (MessageReceipt receipt : unreadReceipts) {
            if (receipt.getDeliveredAt() == null) {
                receipt.setDeliveredAt(now);
            }
            receipt.setReadAt(now);
            messageReceiptRepository.save(receipt);

            notifications.add(new ReceiptNotification(
                    receipt.getMessage().getPublicId(),
                    conversation.getPublicId(),
                    recipient.getPublicId(),
                    "READ",
                    now
            ));
        }

        return notifications;
    }

    @Transactional(readOnly = true)
    public String getMessageSenderUsername(String messagePublicId) {
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));
        return message.getSender().getUsername();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
