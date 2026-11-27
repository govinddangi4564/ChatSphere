package com.chatsphere.conversation;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.conversation.dto.CreatePrivateConversationRequest;
import com.chatsphere.conversation.dto.ConversationDetailsResponse;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.conversation.service.ConversationService;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private UserContactRepository userContactRepository;

    @Mock
    private UserPrivacySettingRepository privacySettingRepository;

    @Mock
    private com.chatsphere.cache.service.RedisCacheService redisCacheService;

    private ConversationService conversationService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                conversationMemberRepository,
                userRepository,
                blockedUserRepository,
                userContactRepository,
                privacySettingRepository,
                redisCacheService
        );

        lenient().when(redisCacheService.getOrCompute(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<?> supplier = inv.getArgument(3);
                    return supplier != null ? supplier.get() : null;
                });

        user1 = new User();
        user1.setId(1L);
        user1.setPublicId(UUID.randomUUID().toString());
        user1.setUsername("alice");
        user1.setFullName("Alice Smith");

        user2 = new User();
        user2.setId(2L);
        user2.setPublicId(UUID.randomUUID().toString());
        user2.setUsername("bob");
        user2.setFullName("Bob Jones");

        user3 = new User();
        user3.setId(3L);
        user3.setPublicId(UUID.randomUUID().toString());
        user3.setUsername("charlie");
        user3.setFullName("Charlie Brown");
    }

    @Test
    void createOrFindPrivateConversation_CannotStartWithSelf() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        given(userRepository.findByPublicId(user1.getPublicId())).willReturn(Optional.of(user1));

        CreatePrivateConversationRequest req = new CreatePrivateConversationRequest(user1.getPublicId());

        assertThatThrownBy(() -> conversationService.createOrFindPrivateConversation("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot create a private conversation with yourself");
    }

    @Test
    void createOrFindPrivateConversation_Blocked_ThrowsResourceNotFound() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        given(userRepository.findByPublicId(user2.getPublicId())).willReturn(Optional.of(user2));
        given(blockedUserRepository.isBidirectionallyBlocked(1L, 2L)).willReturn(true);

        CreatePrivateConversationRequest req = new CreatePrivateConversationRequest(user2.getPublicId());

        assertThatThrownBy(() -> conversationService.createOrFindPrivateConversation("alice", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrFindPrivateConversation_ExistingConversation_ReturnsExisting() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        given(userRepository.findByPublicId(user2.getPublicId())).willReturn(Optional.of(user2));
        given(blockedUserRepository.isBidirectionallyBlocked(1L, 2L)).willReturn(false);

        Conversation existingConv = new Conversation();
        existingConv.setId(10L);
        existingConv.setPublicId(UUID.randomUUID().toString());
        existingConv.setPrivatePairHash("1_2");
        existingConv.setConversationType(ConversationType.PRIVATE);

        given(conversationRepository.findByPrivatePairHash("1_2")).willReturn(Optional.of(existingConv));

        ConversationMember cm1 = new ConversationMember();
        cm1.setId(101L);
        cm1.setConversation(existingConv);
        cm1.setUser(user1);
        cm1.setRole(MemberRole.MEMBER);

        ConversationMember cm2 = new ConversationMember();
        cm2.setId(102L);
        cm2.setConversation(existingConv);
        cm2.setUser(user2);
        cm2.setRole(MemberRole.MEMBER);

        given(conversationMemberRepository.findByConversationPublicIdAndUserId(existingConv.getPublicId(), 1L))
                .willReturn(Optional.of(cm1));
        given(conversationMemberRepository.findAllByConversationId(10L))
                .willReturn(List.of(cm1, cm2));

        CreatePrivateConversationRequest req = new CreatePrivateConversationRequest(user2.getPublicId());
        ConversationDetailsResponse response = conversationService.createOrFindPrivateConversation("alice", req);

        assertThat(response).isNotNull();
        assertThat(response.publicId()).isEqualTo(existingConv.getPublicId());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void createOrFindPrivateConversation_New_CreatesAndReturns() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        given(userRepository.findByPublicId(user2.getPublicId())).willReturn(Optional.of(user2));
        given(blockedUserRepository.isBidirectionallyBlocked(1L, 2L)).willReturn(false);
        given(conversationRepository.findByPrivatePairHash("1_2")).willReturn(Optional.empty());

        Conversation newConv = new Conversation();
        newConv.setId(20L);
        newConv.setPublicId(UUID.randomUUID().toString());
        newConv.setPrivatePairHash("1_2");
        newConv.setConversationType(ConversationType.PRIVATE);

        given(conversationRepository.save(any(Conversation.class))).willReturn(newConv);

        ConversationMember cm1 = new ConversationMember();
        cm1.setConversation(newConv);
        cm1.setUser(user1);

        ConversationMember cm2 = new ConversationMember();
        cm2.setConversation(newConv);
        cm2.setUser(user2);

        given(conversationMemberRepository.findByConversationPublicIdAndUserId(newConv.getPublicId(), 1L))
                .willReturn(Optional.of(cm1));
        given(conversationMemberRepository.findAllByConversationId(20L))
                .willReturn(List.of(cm1, cm2));

        CreatePrivateConversationRequest req = new CreatePrivateConversationRequest(user2.getPublicId());
        ConversationDetailsResponse response = conversationService.createOrFindPrivateConversation("alice", req);

        assertThat(response).isNotNull();
        assertThat(response.publicId()).isEqualTo(newConv.getPublicId());
        verify(conversationRepository).save(any(Conversation.class));
        verify(conversationMemberRepository, times(2)).save(any(ConversationMember.class));
    }

    @Test
    void getConversations_PageSizeExceedsLimit_ThrowsBadRequest() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        Pageable excessivePageable = PageRequest.of(0, 51);

        assertThatThrownBy(() -> conversationService.getConversations("alice", null, null, null, null, excessivePageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size cannot exceed 50");
    }

    @Test
    void getConversations_InvalidSortField_ThrowsBadRequest() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        Pageable invalidSort = PageRequest.of(0, 20, Sort.by("passwordHash"));

        assertThatThrownBy(() -> conversationService.getConversations("alice", null, null, null, null, invalidSort))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    void getConversationDetails_NotMember_ThrowsResourceNotFound() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user1));
        given(conversationMemberRepository.findByConversationPublicIdAndUserId("foreign-conv", 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getConversationDetails("alice", "foreign-conv"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
