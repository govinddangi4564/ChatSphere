package com.chatsphere.message.service;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.attachment.entity.MessageAttachment;
import com.chatsphere.attachment.repository.MessageAttachmentRepository;
import com.chatsphere.attachment.service.AttachmentService;
import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.dto.PaginationMeta;
import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.AppException;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.message.dto.*;
import com.chatsphere.message.entity.Message;
import com.chatsphere.message.entity.MessageDeletion;
import com.chatsphere.message.entity.MessageReaction;
import com.chatsphere.message.entity.MessageReceipt;
import com.chatsphere.message.repository.MessageDeletionRepository;
import com.chatsphere.message.repository.MessageReactionRepository;
import com.chatsphere.message.repository.MessageReceiptRepository;
import com.chatsphere.message.repository.MessageRepository;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserRepository;
import com.chatsphere.websocket.dto.ReceiptNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class MessageService {

    private static final int MAX_PREVIEW_LENGTH = 100;
    private static final Duration DELETE_FOR_EVERYONE_LIMIT = Duration.ofHours(2);

    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MessageDeletionRepository messageDeletionRepository;
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
            MessageReactionRepository messageReactionRepository,
            MessageDeletionRepository messageDeletionRepository,
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
        this.messageReactionRepository = messageReactionRepository;
        this.messageDeletionRepository = messageDeletionRepository;
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

        ConversationMember senderMember = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.conversationPublicId()));

        if (senderMember.getLeftAt() != null) {
            throw new AccessDeniedException("User is not an active member of this conversation");
        }

        List<ConversationMember> allMembers = conversationMemberRepository.findAllByConversationId(conversation.getId());
        List<ConversationMember> activeMembers = allMembers.stream()
                .filter(m -> m.getLeftAt() == null)
                .toList();

        if (conversation.getConversationType() == ConversationType.PRIVATE) {
            ConversationMember recipientMember = activeMembers.stream()
                    .filter(m -> !m.getUser().getId().equals(sender.getId()))
                    .findFirst()
                    .orElse(null);

            if (recipientMember != null && blockedUserRepository.isBidirectionallyBlocked(sender.getId(), recipientMember.getUser().getId())) {
                throw new BadRequestException("Cannot send message: user communication is blocked");
            }
        }

        Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(sender.getId(), request.clientMessageId());
        if (existing.isPresent()) {
            return mapToMessageResponse(existing.get(), sender.getId());
        }

        boolean hasContent = request.content() != null && !request.content().trim().isEmpty();
        boolean hasAttachments = request.attachmentPublicIds() != null && !request.attachmentPublicIds().isEmpty();
        if (!hasContent && !hasAttachments) {
            throw new BadRequestException("Message must have either text content or an attachment");
        }

        Message replyTo = null;
        if (request.replyToMessagePublicId() != null) {
            replyTo = messageRepository.findByPublicId(request.replyToMessagePublicId()).orElse(null);
        }

        Message message = new Message();
        message.setClientMessageId(request.clientMessageId());
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(request.messageType() != null ? request.messageType() : (hasAttachments ? MessageType.IMAGE : MessageType.TEXT));
        message.setContent(hasContent ? request.content().trim() : "");
        message.setReplyToMessage(replyTo);
        message.setCreatedAt(Instant.now());

        Message savedMessage = messageRepository.save(message);

        if (hasAttachments) {
            attachmentService.linkAttachmentsToMessage(savedMessage, request.attachmentPublicIds());
        }

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

        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        String preview = savedMessage.getContent();
        if (preview == null || preview.isEmpty()) {
            preview = "[" + savedMessage.getMessageType().name() + "]";
        } else if (preview.length() > MAX_PREVIEW_LENGTH) {
            preview = preview.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        return mapToMessageResponse(savedMessage, sender.getId());
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

        MessageResponse response = mapToMessageResponse(savedMessage, null);
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversation.getPublicId(),
                response
        );
        return response;
    }

    @Transactional(readOnly = true)
    public CursorApiResponse<MessageResponse> getMessages(
            String currentUsername,
            String conversationPublicId,
            int size,
            String before
    ) {
        User user = getUserByUsername(currentUsername);

        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        if (member.getLeftAt() != null) {
            throw new AccessDeniedException("User is no longer an active member of this conversation");
        }

        if (size > 100) {
            throw new BadRequestException("Maximum page size is 100");
        }
        int pageSize = Math.max(size, 1);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);

        Instant clearedAt = member.getClearedAt();
        List<Message> messages;

        if (before != null && !before.isBlank()) {
            CursorData cursorData = decodeCursor(before);
            messages = new ArrayList<>(messageRepository.findMessagesBeforeCursor(
                    conversation.getId(),
                    clearedAt,
                    cursorData.createdAt(),
                    cursorData.id(),
                    pageRequest
            ));
        } else {
            messages = new ArrayList<>(messageRepository.findNewestMessages(
                    conversation.getId(),
                    clearedAt,
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

        Collections.reverse(messages);

        // Filter out "deleted for me"
        List<Long> messageIds = messages.stream().map(Message::getId).toList();
        Set<Long> deletedMessageIds = new HashSet<>(
                messageDeletionRepository.findAllByUserIdAndMessageIdIn(user.getId(), messageIds)
                        .stream().map(d -> d.getMessage().getId()).toList()
        );

        List<MessageResponse> dtos = messages.stream()
                .filter(m -> !deletedMessageIds.contains(m.getId()))
                .map(m -> mapToMessageResponse(m, user.getId()))
                .toList();

        return CursorApiResponse.of(dtos, nextCursor, hasMore);
    }

    public MessageResponse editMessage(String currentUsername, String messagePublicId, EditMessageRequest request) {
        User user = getUserByUsername(currentUsername);
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        if (!message.getSender().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        if (message.isDeletedForEveryone()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }

        message.setContent(request.content().trim());
        message.setEdited(true);
        message.setUpdatedAt(Instant.now());
        Message updated = messageRepository.save(message);

        MessageResponse response = mapToMessageResponse(updated, user.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getPublicId(),
                Map.of("type", "MESSAGE_EDITED", "message", response)
        );

        return response;
    }

    public void deleteMessage(String currentUsername, String messagePublicId, boolean forEveryone) {
        User user = getUserByUsername(currentUsername);
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        Conversation conversation = message.getConversation();
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));

        if (forEveryone) {
            boolean isSender = message.getSender().getId().equals(user.getId());
            boolean isAdmin = member.getRole() == MemberRole.ADMIN && member.getLeftAt() == null;

            if (!isSender && !isAdmin) {
                throw new AccessDeniedException("Only the sender or a group admin can delete a message for everyone");
            }

            Duration age = Duration.between(message.getCreatedAt(), Instant.now());
            if (age.compareTo(DELETE_FOR_EVERYONE_LIMIT) > 0) {
                throw new BadRequestException("Messages can only be deleted for everyone within " + DELETE_FOR_EVERYONE_LIMIT.toHours() + " hours");
            }

            message.setDeletedForEveryone(true);
            message.setContent("This message was deleted");
            message.setUpdatedAt(Instant.now());
            messageRepository.save(message);

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + conversation.getPublicId(),
                    Map.of("type", "MESSAGE_DELETED", "messagePublicId", message.getPublicId(), "forEveryone", true)
            );
        } else {
            // Delete for me
            if (!messageDeletionRepository.existsByMessageIdAndUserId(message.getId(), user.getId())) {
                MessageDeletion deletion = MessageDeletion.builder()
                        .message(message)
                        .user(user)
                        .build();
                messageDeletionRepository.save(deletion);
            }
        }
    }

    public MessageResponse forwardMessage(String currentUsername, String messagePublicId, ForwardMessageRequest request) {
        User user = getUserByUsername(currentUsername);
        Message sourceMessage = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        // Validate sender is member of source conversation
        conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(sourceMessage.getConversation().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not permitted to forward from this conversation"));

        // Validate membership in target conversation
        Conversation targetConv = conversationRepository.findByPublicId(request.targetConversationPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", request.targetConversationPublicId()));

        ConversationMember targetMember = conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(targetConv.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of the target conversation"));

        // Validate block rules for target if private
        if (targetConv.getConversationType() == ConversationType.PRIVATE) {
            List<ConversationMember> members = conversationMemberRepository.findAllByConversationId(targetConv.getId());
            members.stream()
                    .filter(m -> !m.getUser().getId().equals(user.getId()) && m.getLeftAt() == null)
                    .findFirst()
                    .ifPresent(other -> {
                        if (blockedUserRepository.isBidirectionallyBlocked(user.getId(), other.getUser().getId())) {
                            throw new BadRequestException("Cannot forward message: communication is blocked with recipient");
                        }
                    });
        }

        // Create forwarded message
        String clientMsgId = "fwd-" + UUID.randomUUID();
        Message forwarded = new Message();
        forwarded.setClientMessageId(clientMsgId);
        forwarded.setConversation(targetConv);
        forwarded.setSender(user);
        forwarded.setMessageType(sourceMessage.getMessageType());
        forwarded.setContent(sourceMessage.getContent());
        forwarded.setForwardedFromMessageId(sourceMessage.getId());
        forwarded.setCreatedAt(Instant.now());

        Message saved = messageRepository.save(forwarded);

        // Copy attachments to new message if any
        List<MessageAttachment> sourceAttachments = messageAttachmentRepository.findAllByMessageId(sourceMessage.getId());
        for (MessageAttachment att : sourceAttachments) {
            MessageAttachment copy = MessageAttachment.builder()
                    .publicId(UUID.randomUUID().toString())
                    .message(saved)
                    .storagePublicId(att.getStoragePublicId())
                    .fileName(att.getFileName())
                    .fileType(att.getFileType())
                    .mimeType(att.getMimeType())
                    .fileSize(att.getFileSize())
                    .storageUrl(att.getStorageUrl())
                    .thumbnailUrl(att.getThumbnailUrl())
                    .build();
            messageAttachmentRepository.save(copy);
        }

        // Target member unread counts & receipts
        List<ConversationMember> activeMembers = conversationMemberRepository.findAllByConversationId(targetConv.getId()).stream()
                .filter(m -> m.getLeftAt() == null)
                .toList();

        for (ConversationMember m : activeMembers) {
            if (!m.getUser().getId().equals(user.getId())) {
                MessageReceipt receipt = new MessageReceipt();
                receipt.setMessage(saved);
                receipt.setRecipient(m.getUser());
                messageReceiptRepository.save(receipt);

                m.setUnreadCount(m.getUnreadCount() + 1);
                conversationMemberRepository.save(m);
            }
        }

        targetConv.setLastMessageAt(saved.getCreatedAt());
        targetConv.setLastMessagePreview(saved.getContent());
        conversationRepository.save(targetConv);

        MessageResponse response = mapToMessageResponse(saved, user.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + targetConv.getPublicId(),
                response
        );

        return response;
    }

    public MessageResponse addReaction(String currentUsername, String messagePublicId, ReactionRequest request) {
        User user = getUserByUsername(currentUsername);
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(message.getConversation().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));

        Optional<MessageReaction> existing = messageReactionRepository.findByMessageIdAndUserId(message.getId(), user.getId());
        if (existing.isPresent()) {
            MessageReaction r = existing.get();
            r.setEmoji(request.emoji());
            messageReactionRepository.save(r);
        } else {
            MessageReaction reaction = MessageReaction.builder()
                    .message(message)
                    .user(user)
                    .emoji(request.emoji())
                    .build();
            messageReactionRepository.save(reaction);
        }

        MessageResponse response = mapToMessageResponse(message, user.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getPublicId(),
                Map.of("type", "MESSAGE_REACTION", "message", response)
        );

        return response;
    }

    public MessageResponse removeReaction(String currentUsername, String messagePublicId) {
        User user = getUserByUsername(currentUsername);
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(message.getConversation().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));

        messageReactionRepository.deleteByMessageIdAndUserId(message.getId(), user.getId());

        MessageResponse response = mapToMessageResponse(message, user.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getPublicId(),
                Map.of("type", "MESSAGE_REACTION", "message", response)
        );

        return response;
    }

    public MessageResponse pinMessage(String currentUsername, String messagePublicId, boolean pin) {
        User user = getUserByUsername(currentUsername);
        Message message = messageRepository.findByPublicId(messagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "publicId", messagePublicId));

        conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(message.getConversation().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));

        message.setPinned(pin);
        message.setPinnedAt(pin ? Instant.now() : null);
        message.setPinnedBy(pin ? user : null);
        Message saved = messageRepository.save(message);

        MessageResponse response = mapToMessageResponse(saved, user.getId());
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getPublicId(),
                Map.of("type", "MESSAGE_PINNED", "message", response)
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getPinnedMessages(String currentUsername, String conversationPublicId) {
        User user = getUserByUsername(currentUsername);
        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));

        List<Message> pinned = messageRepository.findPinnedMessages(conversation.getId());
        return pinned.stream().map(m -> mapToMessageResponse(m, user.getId())).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> searchMessages(
            String currentUsername,
            String conversationPublicId,
            String query,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        User user = getUserByUsername(currentUsername);

        List<Long> accessibleConversationIds;
        if (conversationPublicId != null && !conversationPublicId.isBlank()) {
            Conversation conv = conversationRepository.findByPublicId(conversationPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

            conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(conv.getId(), user.getId())
                    .orElseThrow(() -> new AccessDeniedException("Not a member of this conversation"));
            accessibleConversationIds = List.of(conv.getId());
        } else {
            accessibleConversationIds = conversationMemberRepository.findAllByUserId(user.getId()).stream()
                    .filter(m -> m.getLeftAt() == null)
                    .map(m -> m.getConversation().getId())
                    .toList();
        }

        if (accessibleConversationIds.isEmpty()) {
            return PageResponse.of(List.of(), new PaginationMeta(0, pageable.getPageSize(), 0, 0, false, false));
        }

        Page<Message> searchPage = messageRepository.searchMessages(
                accessibleConversationIds,
                query.trim(),
                startDate,
                endDate,
                pageable
        );

        List<MessageResponse> dtos = searchPage.getContent().stream()
                .map(m -> mapToMessageResponse(m, user.getId()))
                .toList();

        return PageResponse.of(
                dtos,
                new PaginationMeta(
                        searchPage.getNumber(),
                        searchPage.getSize(),
                        searchPage.getTotalElements(),
                        searchPage.getTotalPages(),
                        searchPage.hasNext(),
                        searchPage.hasPrevious()
                )
        );
    }

    public void deleteConversationForMe(String currentUsername, String conversationPublicId) {
        User user = getUserByUsername(currentUsername);
        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversation.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        member.setClearedAt(Instant.now());
        member.setUnreadCount(0);
        conversationMemberRepository.save(member);
    }

    public MessageResponse mapToMessageResponse(Message message) {
        return mapToMessageResponse(message, null);
    }

    public MessageResponse mapToMessageResponse(Message message, Long currentUserId) {
        List<AttachmentResponse> attachments = List.of();
        if (message.getId() != null) {
            List<MessageAttachment> attEntities = messageAttachmentRepository.findAllByMessageId(message.getId());
            if (attEntities != null && !attEntities.isEmpty()) {
                attachments = attEntities.stream()
                        .map(attachmentService::toResponse)
                        .toList();
            }
        }

        ReplySnippet replySnippet = null;
        if (message.getReplyToMessage() != null) {
            Message r = message.getReplyToMessage();
            replySnippet = new ReplySnippet(
                    r.getPublicId(),
                    r.getSender().getPublicId(),
                    r.getSender().getUsername(),
                    r.getSender().getFullName(),
                    r.isDeletedForEveryone() ? "This message was deleted" : r.getContent(),
                    r.getMessageType().name()
            );
        }

        List<ReactionResponse> reactions = List.of();
        if (message.getId() != null) {
            reactions = messageReactionRepository.findAllByMessageId(message.getId()).stream()
                    .map(r -> new ReactionResponse(
                            r.getEmoji(),
                            r.getUser().getPublicId(),
                            r.getUser().getUsername(),
                            r.getCreatedAt()
                    ))
                    .toList();
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
                attachments,
                replySnippet,
                message.getForwardedFromMessageId() != null,
                message.isPinned(),
                message.getPinnedAt(),
                message.getPinnedBy() != null ? message.getPinnedBy().getUsername() : null,
                reactions
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
            throw new BadRequestException("Invalid cursor format");
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
        return markMessagesRead(currentUsername, conversationPublicId);
    }

    public List<ReceiptNotification> markMessagesDelivered(String recipientUsername, String conversationPublicId) {
        User recipient = getUserByUsername(recipientUsername);
        Conversation conversation = conversationRepository.findByPublicId(conversationPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", conversationPublicId));

        List<MessageReceipt> undeliveredReceipts = messageReceiptRepository.findUndeliveredReceiptsForConversation(
                conversation.getId(),
                recipient.getId()
        );

        Instant now = Instant.now();
        List<ReceiptNotification> notifications = new ArrayList<>();

        for (MessageReceipt receipt : undeliveredReceipts) {
            receipt.setDeliveredAt(now);
            messageReceiptRepository.save(receipt);

            notifications.add(new ReceiptNotification(
                    receipt.getMessage().getPublicId(),
                    conversation.getPublicId(),
                    recipient.getPublicId(),
                    "DELIVERED",
                    now
            ));
        }

        return notifications;
    }

    public List<ReceiptNotification> markMessagesRead(String recipientUsername, String conversationPublicId) {
        User recipient = getUserByUsername(recipientUsername);
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
