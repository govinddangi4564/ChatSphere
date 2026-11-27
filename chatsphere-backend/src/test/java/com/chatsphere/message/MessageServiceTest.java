package com.chatsphere.message;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
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
import com.chatsphere.message.service.MessageService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReceiptRepository messageReceiptRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Mock
    private com.chatsphere.message.repository.MessageReactionRepository messageReactionRepository;

    @Mock
    private com.chatsphere.message.repository.MessageDeletionRepository messageDeletionRepository;

    @Mock
    private com.chatsphere.attachment.service.AttachmentService attachmentService;

    @Mock
    private com.chatsphere.attachment.repository.MessageAttachmentRepository messageAttachmentRepository;

    @Mock
    private com.chatsphere.cache.service.RedisCacheService redisCacheService;

    @Mock
    private com.chatsphere.notification.service.NotificationService notificationService;

    @Mock
    private com.chatsphere.websocket.service.PresenceService presenceService;

    private MessageService messageService;

    private User sender;
    private User recipient;
    private Conversation conversation;
    private ConversationMember senderMember;
    private ConversationMember recipientMember;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository,
                messageReceiptRepository,
                messageReactionRepository,
                messageDeletionRepository,
                conversationRepository,
                conversationMemberRepository,
                userRepository,
                blockedUserRepository,
                messagingTemplate,
                attachmentService,
                messageAttachmentRepository,
                redisCacheService,
                notificationService,
                presenceService
        );

        sender = new User();
        sender.setId(1L);
        sender.setPublicId(UUID.randomUUID().toString());
        sender.setUsername("alice");
        sender.setFullName("Alice Johnson");

        recipient = new User();
        recipient.setId(2L);
        recipient.setPublicId(UUID.randomUUID().toString());
        recipient.setUsername("bob");
        recipient.setFullName("Bob Jones");

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setPublicId(UUID.randomUUID().toString());
        conversation.setConversationType(ConversationType.PRIVATE);

        senderMember = new ConversationMember();
        senderMember.setId(101L);
        senderMember.setUser(sender);
        senderMember.setConversation(conversation);
        senderMember.setRole(MemberRole.MEMBER);
        senderMember.setUnreadCount(0);

        recipientMember = new ConversationMember();
        recipientMember.setId(102L);
        recipientMember.setUser(recipient);
        recipientMember.setConversation(conversation);
        recipientMember.setRole(MemberRole.MEMBER);
        recipientMember.setUnreadCount(0);
    }

    @Test
    void sendMessage_Success() {
        SendMessageRequest request = new SendMessageRequest(
                conversation.getPublicId(),
                "client-msg-123",
                "Hello Bob!",
                MessageType.TEXT
        );

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.of(senderMember));
        given(conversationMemberRepository.findAllByConversationId(10L)).willReturn(List.of(senderMember, recipientMember));
        given(blockedUserRepository.isBidirectionallyBlocked(1L, 2L)).willReturn(false);
        given(messageRepository.findBySenderIdAndClientMessageId(1L, "client-msg-123")).willReturn(Optional.empty());

        Message savedMsg = new Message();
        savedMsg.setId(500L);
        savedMsg.setPublicId(UUID.randomUUID().toString());
        savedMsg.setClientMessageId("client-msg-123");
        savedMsg.setConversation(conversation);
        savedMsg.setSender(sender);
        savedMsg.setContent("Hello Bob!");
        savedMsg.setMessageType(MessageType.TEXT);
        savedMsg.setCreatedAt(Instant.now());

        given(messageRepository.save(any(Message.class))).willReturn(savedMsg);

        MessageResponse response = messageService.sendMessage("alice", request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Hello Bob!");
        assertThat(response.clientMessageId()).isEqualTo("client-msg-123");
        assertThat(response.senderUsername()).isEqualTo("alice");

        // Verify receipt created for recipient
        verify(messageReceiptRepository).save(any(MessageReceipt.class));
        // Verify recipient unread count incremented
        assertThat(recipientMember.getUnreadCount()).isEqualTo(1);
        verify(conversationMemberRepository).save(recipientMember);
        // Verify sender unread count remains 0
        assertThat(senderMember.getUnreadCount()).isEqualTo(0);
        // Verify conversation last message updated
        assertThat(conversation.getLastMessagePreview()).isEqualTo("Hello Bob!");
        assertThat(conversation.getLastMessageAt()).isNotNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    void sendMessage_DuplicateClientMessageId_ReturnsExistingWithoutRecreating() {
        SendMessageRequest request = new SendMessageRequest(
                conversation.getPublicId(),
                "client-msg-dup",
                "Duplicate test message",
                MessageType.TEXT
        );

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.of(senderMember));
        given(conversationMemberRepository.findAllByConversationId(10L)).willReturn(List.of(senderMember, recipientMember));
        given(blockedUserRepository.isBidirectionallyBlocked(1L, 2L)).willReturn(false);

        Message existingMsg = new Message();
        existingMsg.setId(600L);
        existingMsg.setPublicId(UUID.randomUUID().toString());
        existingMsg.setClientMessageId("client-msg-dup");
        existingMsg.setConversation(conversation);
        existingMsg.setSender(sender);
        existingMsg.setContent("Duplicate test message");
        existingMsg.setMessageType(MessageType.TEXT);
        existingMsg.setCreatedAt(Instant.now());

        given(messageRepository.findBySenderIdAndClientMessageId(1L, "client-msg-dup")).willReturn(Optional.of(existingMsg));

        MessageResponse response = messageService.sendMessage("alice", request);

        assertThat(response).isNotNull();
        assertThat(response.publicId()).isEqualTo(existingMsg.getPublicId());
        // Verify save was never called
        verify(messageRepository, never()).save(any(Message.class));
        verify(messageReceiptRepository, never()).save(any(MessageReceipt.class));
        assertThat(recipientMember.getUnreadCount()).isEqualTo(0);
    }

    @Test
    void sendMessage_NonMember_ThrowsResourceNotFoundException() {
        SendMessageRequest request = new SendMessageRequest(
                conversation.getPublicId(),
                "client-msg-456",
                "Unauthorized attempt",
                MessageType.TEXT
        );

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage("alice", request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void getMessages_NonMember_ThrowsResourceNotFoundException() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getMessages("alice", conversation.getPublicId(), 30, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMessages_ExceedsMaxPageSize_ThrowsBadRequest() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.of(senderMember));

        assertThatThrownBy(() -> messageService.getMessages("alice", conversation.getPublicId(), 101, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum page size is 100");
    }

    @Test
    void getMessages_CursorPagination_IdenticalTimestamps() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(sender));
        given(conversationRepository.findByPublicId(conversation.getPublicId())).willReturn(Optional.of(conversation));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).willReturn(Optional.of(senderMember));

        Instant sameTimestamp = Instant.parse("2026-11-26T12:00:00Z");

        // DB returns in DESC order (limit = 2 + 1 = 3): m3 (newest), m2, m1 (oldest)
        Message m1 = createMessage(10L, "First", sameTimestamp);
        Message m2 = createMessage(11L, "Second", sameTimestamp);
        Message m3 = createMessage(12L, "Third", sameTimestamp);

        given(messageRepository.findNewestMessages(eq(10L), any(), any()))
                .willReturn(new ArrayList<>(List.of(m3, m2, m1)));

        CursorApiResponse<MessageResponse> response = messageService.getMessages(
                "alice",
                conversation.getPublicId(),
                2,
                null
        );

        assertThat(response.success()).isTrue();
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotNull();

        // The boundary item m1 should be removed so exactly 2 messages (m2, m3) are returned in chronological order
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).content()).isEqualTo("Second");
        assertThat(response.data().get(1).content()).isEqualTo("Third");

        // Verify the cursor can be decoded back to boundary m1's timestamp and id 10
        MessageService.CursorData cursorData = MessageService.decodeCursor(response.nextCursor());
        assertThat(cursorData.createdAt()).isEqualTo(sameTimestamp);
        assertThat(cursorData.id()).isEqualTo(10L);
    }

    private Message createMessage(Long id, String content, Instant createdAt) {
        Message m = new Message();
        m.setId(id);
        m.setPublicId(UUID.randomUUID().toString());
        m.setClientMessageId("client-" + id);
        m.setConversation(conversation);
        m.setSender(sender);
        m.setContent(content);
        m.setMessageType(MessageType.TEXT);
        m.setCreatedAt(createdAt);
        return m;
    }
}
