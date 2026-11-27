package com.chatsphere.group;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.group.dto.*;
import com.chatsphere.group.entity.GroupDetail;
import com.chatsphere.group.repository.GroupDetailRepository;
import com.chatsphere.group.service.GroupService;
import com.chatsphere.message.service.MessageService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
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
class GroupServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private GroupDetailRepository groupDetailRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private com.chatsphere.notification.service.NotificationService notificationService;

    private GroupService groupService;

    private User adminUser;
    private User regularUser;
    private User newUser;
    private Conversation groupConversation;
    private GroupDetail groupDetail;
    private ConversationMember adminMember;
    private ConversationMember regularMember;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(
                conversationRepository,
                conversationMemberRepository,
                groupDetailRepository,
                userRepository,
                messageService,
                notificationService
        );

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setPublicId("u-admin");
        adminUser.setUsername("alice");
        adminUser.setFullName("Alice Admin");

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setPublicId("u-bob");
        regularUser.setUsername("bob");
        regularUser.setFullName("Bob Member");

        newUser = new User();
        newUser.setId(3L);
        newUser.setPublicId("u-charlie");
        newUser.setUsername("charlie");
        newUser.setFullName("Charlie New");

        groupConversation = new Conversation();
        groupConversation.setId(10L);
        groupConversation.setPublicId("grp-100");
        groupConversation.setConversationType(ConversationType.GROUP);
        groupConversation.setTitle("Engineering Team");
        groupConversation.setImageUrl(null);
        groupConversation.setCreatedBy(adminUser);

        groupDetail = new GroupDetail();
        groupDetail.setId(5L);
        groupDetail.setConversation(groupConversation);
        groupDetail.setDescription("Discussion for engineering topics");
        groupDetail.setCreatedBy(adminUser);

        adminMember = new ConversationMember();
        adminMember.setId(101L);
        adminMember.setConversation(groupConversation);
        adminMember.setUser(adminUser);
        adminMember.setRole(MemberRole.ADMIN);
        adminMember.setJoinedAt(Instant.now());

        regularMember = new ConversationMember();
        regularMember.setId(102L);
        regularMember.setConversation(groupConversation);
        regularMember.setUser(regularUser);
        regularMember.setRole(MemberRole.MEMBER);
        regularMember.setJoinedAt(Instant.now().plusSeconds(1));
    }

    @Test
    void createGroup_Success() {
        CreateGroupRequest request = new CreateGroupRequest(
                "Engineering Team",
                "Discussion for engineering topics",
                null,
                List.of("u-bob")
        );

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(userRepository.findByPublicId("u-bob")).willReturn(Optional.of(regularUser));
        given(conversationRepository.save(any(Conversation.class))).willReturn(groupConversation);
        given(groupDetailRepository.save(any(GroupDetail.class))).willReturn(groupDetail);

        // For getGroupDetails call within createGroup:
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(groupDetailRepository.findByConversationId(10L)).willReturn(Optional.of(groupDetail));
        given(conversationMemberRepository.findAllActiveByConversationId(10L))
                .willReturn(List.of(adminMember, regularMember));

        GroupDetailsResponse response = groupService.createGroup("alice", request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Engineering Team");
        assertThat(response.members()).hasSize(2);
        assertThat(response.currentUserRole()).isEqualTo(MemberRole.ADMIN);

        verify(messageService).sendSystemMessage(eq(groupConversation), contains("created group"));
    }

    @Test
    void getGroupDetails_ActiveMember_Success() {
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(regularUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 2L))
                .willReturn(Optional.of(regularMember));
        given(groupDetailRepository.findByConversationId(10L)).willReturn(Optional.of(groupDetail));
        given(conversationMemberRepository.findAllActiveByConversationId(10L))
                .willReturn(List.of(adminMember, regularMember));

        GroupDetailsResponse response = groupService.getGroupDetails("bob", "grp-100");

        assertThat(response.currentUserRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(response.members()).hasSize(2);
    }

    @Test
    void getGroupDetails_NonMember_ThrowsAccessDenied() {
        given(userRepository.findByUsername("charlie")).willReturn(Optional.of(newUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 3L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupDetails("charlie", "grp-100"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not an active member");
    }

    @Test
    void updateGroup_AsAdmin_Success() {
        UpdateGroupRequest request = new UpdateGroupRequest("DevOps Team", "New description", "http://image.png");

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(groupDetailRepository.findByConversationId(10L)).willReturn(Optional.of(groupDetail));
        given(conversationMemberRepository.findAllActiveByConversationId(10L))
                .willReturn(List.of(adminMember, regularMember));

        GroupDetailsResponse response = groupService.updateGroup("alice", "grp-100", request);

        assertThat(groupConversation.getTitle()).isEqualTo("DevOps Team");
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("changed the group name"));
    }

    @Test
    void updateGroup_AsRegularMember_ThrowsAccessDenied() {
        UpdateGroupRequest request = new UpdateGroupRequest("Hacked Group", null, null);

        given(userRepository.findByUsername("bob")).willReturn(Optional.of(regularUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 2L))
                .willReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> groupService.updateGroup("bob", "grp-100", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only group admins can perform this action");
    }

    @Test
    void addMembers_AsAdmin_Success() {
        AddGroupMembersRequest request = new AddGroupMembersRequest(List.of("u-charlie"));

        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(userRepository.findByPublicId("u-charlie")).willReturn(Optional.of(newUser));
        given(conversationMemberRepository.findByConversationIdAndUserId(10L, 3L)).willReturn(Optional.empty());

        given(groupDetailRepository.findByConversationId(10L)).willReturn(Optional.of(groupDetail));
        given(conversationMemberRepository.findAllActiveByConversationId(10L))
                .willReturn(List.of(adminMember, regularMember));

        groupService.addMembers("alice", "grp-100", request);

        verify(conversationMemberRepository).save(any(ConversationMember.class));
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("added Charlie New"));
    }

    @Test
    void removeMember_AsAdmin_Success() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(userRepository.findByPublicId("u-bob")).willReturn(Optional.of(regularUser));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 2L))
                .willReturn(Optional.of(regularMember));

        groupService.removeMember("alice", "grp-100", "u-bob");

        assertThat(regularMember.getLeftAt()).isNotNull();
        verify(conversationMemberRepository).save(regularMember);
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("removed Bob Member"));
    }

    @Test
    void removeMember_AdminRemovesSelf_ThrowsBadRequest() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(userRepository.findByPublicId("u-admin")).willReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> groupService.removeMember("alice", "grp-100", "u-admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("use leave group instead");
    }

    @Test
    void leaveGroup_LastAdminLeaves_AutoPromotesOldestMember() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(conversationMemberRepository.countActiveAdminsByConversationId(10L)).willReturn(0L);
        given(conversationMemberRepository.findFirstByConversationIdAndLeftAtIsNullOrderByJoinedAtAsc(10L))
                .willReturn(Optional.of(regularMember));

        groupService.leaveGroup("alice", "grp-100");

        assertThat(adminMember.getLeftAt()).isNotNull();
        assertThat(regularMember.getRole()).isEqualTo(MemberRole.ADMIN);
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("promoted to admin"));
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("left the group"));
    }

    @Test
    void updateMemberRole_PromoteAndDemote() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(adminUser));
        given(conversationRepository.findByPublicId("grp-100")).willReturn(Optional.of(groupConversation));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 1L))
                .willReturn(Optional.of(adminMember));
        given(userRepository.findByPublicId("u-bob")).willReturn(Optional.of(regularUser));
        given(conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(10L, 2L))
                .willReturn(Optional.of(regularMember));
        given(groupDetailRepository.findByConversationId(10L)).willReturn(Optional.of(groupDetail));
        given(conversationMemberRepository.findAllActiveByConversationId(10L))
                .willReturn(List.of(adminMember, regularMember));

        // Promote to admin
        groupService.updateMemberRole("alice", "grp-100", "u-bob", new UpdateMemberRoleRequest(MemberRole.ADMIN));
        assertThat(regularMember.getRole()).isEqualTo(MemberRole.ADMIN);
        verify(messageService).sendSystemMessage(eq(groupConversation), contains("made Bob Member an admin"));
    }
}
