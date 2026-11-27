package com.chatsphere.message.service;

import com.chatsphere.attachment.repository.MessageAttachmentRepository;
import com.chatsphere.attachment.service.AttachmentService;
import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.message.dto.EditMessageRequest;
import com.chatsphere.message.dto.MessageResponse;
import com.chatsphere.message.dto.ReactionRequest;
import com.chatsphere.message.entity.Message;
import com.chatsphere.message.entity.MessageReaction;
import com.chatsphere.message.repository.MessageDeletionRepository;
import com.chatsphere.message.repository.MessageReactionRepository;
import com.chatsphere.message.repository.MessageReceiptRepository;
import com.chatsphere.message.repository.MessageRepository;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageManagementTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReceiptRepository messageReceiptRepository;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private MessageDeletionRepository messageDeletionRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private MessageAttachmentRepository messageAttachmentRepository;

    @Mock
    private com.chatsphere.cache.service.RedisCacheService redisCacheService;

    @Mock
    private com.chatsphere.notification.service.NotificationService notificationService;

    @Mock
    private com.chatsphere.websocket.service.PresenceService presenceService;

    private MessageService messageService;

    private User alice;
    private User bob;
    private Conversation conversation;
    private Message message;

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

        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setPublicId("conv-10");
        conversation.setConversationType(ConversationType.PRIVATE);

        message = new Message();
        message.setId(100L);
        message.setPublicId("msg-100");
        message.setSender(alice);
        message.setConversation(conversation);
        message.setContent("Hello Alice");
        message.setCreatedAt(Instant.now());
    }

    @Test
    void editMessage_ownMessage_succeeds() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse response = messageService.editMessage("alice", "msg-100", new EditMessageRequest("Updated text"));

        assertNotNull(response);
        assertEquals("Updated text", message.getContent());
        assertTrue(message.isEdited());
    }

    @Test
    void editMessage_otherUserMessage_throwsAccessDenied() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        assertThrows(AccessDeniedException.class, () ->
                messageService.editMessage("bob", "msg-100", new EditMessageRequest("Hacked text"))
        );
    }

    @Test
    void deleteMessage_forEveryone_withinTimeLimit_succeeds() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        ConversationMember member = new ConversationMember();
        member.setUser(alice);
        member.setRole(MemberRole.MEMBER);
        when(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        messageService.deleteMessage("alice", "msg-100", true);

        assertTrue(message.isDeletedForEveryone());
        assertEquals("This message was deleted", message.getContent());
        verify(messageRepository).save(message);
    }

    @Test
    void deleteMessage_forEveryone_expiredTimeLimit_throwsBadRequest() {
        // Message created 3 hours ago (exceeds 2 hour limit)
        message.setCreatedAt(Instant.now().minus(3, ChronoUnit.HOURS));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        ConversationMember member = new ConversationMember();
        member.setUser(alice);
        member.setRole(MemberRole.MEMBER);
        when(conversationMemberRepository.findByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        assertThrows(BadRequestException.class, () ->
                messageService.deleteMessage("alice", "msg-100", true)
        );
    }

    @Test
    void deleteMessage_forMe_savesDeletionRecord() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        ConversationMember member = new ConversationMember();
        member.setUser(bob);
        when(conversationMemberRepository.findByConversationIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(messageDeletionRepository.existsByMessageIdAndUserId(100L, 2L)).thenReturn(false);

        messageService.deleteMessage("bob", "msg-100", false);

        verify(messageDeletionRepository).save(any());
        // Message remains not deleted for everyone
        assertFalse(message.isDeletedForEveryone());
    }

    @Test
    void addReaction_memberUser_succeeds() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        ConversationMember member = new ConversationMember();
        member.setUser(bob);
        when(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 2L)).thenReturn(Optional.of(member));
        when(messageReactionRepository.findByMessageIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        MessageResponse response = messageService.addReaction("bob", "msg-100", new ReactionRequest("👍"));

        assertNotNull(response);
        verify(messageReactionRepository).save(any());
    }

    @Test
    void pinMessage_member_togglesPin() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(messageRepository.findByPublicId("msg-100")).thenReturn(Optional.of(message));

        ConversationMember member = new ConversationMember();
        member.setUser(alice);
        when(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L)).thenReturn(Optional.of(member));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse response = messageService.pinMessage("alice", "msg-100", true);

        assertTrue(message.isPinned());
        assertNotNull(message.getPinnedAt());
    }
}
