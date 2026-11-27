package com.chatsphere.group.service;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.group.dto.*;
import com.chatsphere.group.entity.GroupDetail;
import com.chatsphere.group.repository.GroupDetailRepository;
import com.chatsphere.message.service.MessageService;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GroupService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final GroupDetailRepository groupDetailRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public GroupService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            GroupDetailRepository groupDetailRepository,
            UserRepository userRepository,
            MessageService messageService
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.groupDetailRepository = groupDetailRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    public GroupDetailsResponse createGroup(String currentUsername, CreateGroupRequest request) {
        User creator = getUserByUsername(currentUsername);

        // 1. Create Conversation of type GROUP
        Conversation conversation = new Conversation();
        conversation.setConversationType(ConversationType.GROUP);
        conversation.setTitle(request.title().trim());
        conversation.setImageUrl(request.imageUrl());
        conversation.setCreatedBy(creator);
        Conversation savedConversation = conversationRepository.save(conversation);

        // 2. Create GroupDetail
        GroupDetail groupDetail = new GroupDetail();
        groupDetail.setConversation(savedConversation);
        groupDetail.setDescription(request.description() != null ? request.description().trim() : null);
        groupDetail.setCreatedBy(creator);
        groupDetailRepository.save(groupDetail);

        // 3. Add Creator as ADMIN
        ConversationMember creatorMember = new ConversationMember();
        creatorMember.setConversation(savedConversation);
        creatorMember.setUser(creator);
        creatorMember.setRole(MemberRole.ADMIN);
        conversationMemberRepository.save(creatorMember);

        // 4. Add Initial Members
        if (request.memberPublicIds() != null) {
            for (String memberPublicId : request.memberPublicIds()) {
                if (memberPublicId.equals(creator.getPublicId())) {
                    continue;
                }
                User memberUser = getUserByPublicId(memberPublicId);
                ConversationMember member = new ConversationMember();
                member.setConversation(savedConversation);
                member.setUser(memberUser);
                member.setRole(MemberRole.MEMBER);
                conversationMemberRepository.save(member);
            }
        }

        // 5. Dispatch initial SYSTEM message
        String systemContent = creator.getFullName() + " created group \"" + savedConversation.getTitle() + "\"";
        messageService.sendSystemMessage(savedConversation, systemContent);

        return getGroupDetails(currentUsername, savedConversation.getPublicId());
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(String currentUsername, String groupPublicId) {
        User currentUser = getUserByUsername(currentUsername);
        Conversation conversation = getGroupConversation(groupPublicId);

        ConversationMember currentMembership = conversationMemberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of this group"));

        GroupDetail groupDetail = groupDetailRepository.findByConversationId(conversation.getId()).orElse(null);

        List<ConversationMember> activeMembers = conversationMemberRepository.findAllActiveByConversationId(conversation.getId());
        List<GroupMemberResponse> memberResponses = activeMembers.stream()
                .map(m -> new GroupMemberResponse(
                        m.getUser().getPublicId(),
                        m.getUser().getUsername(),
                        m.getUser().getFullName(),
                        m.getUser().getProfileImageUrl(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();

        User creator = conversation.getCreatedBy();
        GroupCreatorResponse creatorSummary = new GroupCreatorResponse(
                creator.getPublicId(),
                creator.getUsername(),
                creator.getFullName(),
                creator.getProfileImageUrl()
        );

        return new GroupDetailsResponse(
                conversation.getPublicId(),
                conversation.getTitle(),
                groupDetail != null ? groupDetail.getDescription() : null,
                conversation.getImageUrl(),
                conversation.getCreatedAt(),
                creatorSummary,
                currentMembership.getRole(),
                memberResponses
        );
    }

    public GroupDetailsResponse updateGroup(String currentUsername, String groupPublicId, UpdateGroupRequest request) {
        ConversationMember adminMember = getActiveAdminMember(currentUsername, groupPublicId);
        Conversation conversation = adminMember.getConversation();

        boolean titleChanged = !conversation.getTitle().equals(request.title().trim());
        conversation.setTitle(request.title().trim());
        conversation.setImageUrl(request.imageUrl());
        conversationRepository.save(conversation);

        GroupDetail groupDetail = groupDetailRepository.findByConversationId(conversation.getId())
                .orElseGet(() -> {
                    GroupDetail gd = new GroupDetail();
                    gd.setConversation(conversation);
                    gd.setCreatedBy(adminMember.getUser());
                    return gd;
                });
        groupDetail.setDescription(request.description() != null ? request.description().trim() : null);
        groupDetailRepository.save(groupDetail);

        if (titleChanged) {
            String systemContent = adminMember.getUser().getFullName() + " changed the group name to \"" + conversation.getTitle() + "\"";
            messageService.sendSystemMessage(conversation, systemContent);
        }

        return getGroupDetails(currentUsername, groupPublicId);
    }

    public GroupDetailsResponse addMembers(String currentUsername, String groupPublicId, AddGroupMembersRequest request) {
        ConversationMember adminMember = getActiveAdminMember(currentUsername, groupPublicId);
        Conversation conversation = adminMember.getConversation();

        for (String memberPublicId : request.memberPublicIds()) {
            User targetUser = getUserByPublicId(memberPublicId);

            Optional<ConversationMember> existingOpt = conversationMemberRepository
                    .findByConversationIdAndUserId(conversation.getId(), targetUser.getId());

            if (existingOpt.isPresent()) {
                ConversationMember existing = existingOpt.get();
                if (existing.getLeftAt() == null) {
                    continue; // Already active member
                }
                // Member previously left or was removed -> re-join
                existing.setLeftAt(null);
                existing.setRole(MemberRole.MEMBER);
                existing.setJoinedAt(Instant.now());
                existing.setUnreadCount(0);
                conversationMemberRepository.save(existing);
            } else {
                ConversationMember newMember = new ConversationMember();
                newMember.setConversation(conversation);
                newMember.setUser(targetUser);
                newMember.setRole(MemberRole.MEMBER);
                conversationMemberRepository.save(newMember);
            }

            String systemContent = adminMember.getUser().getFullName() + " added " + targetUser.getFullName();
            messageService.sendSystemMessage(conversation, systemContent);
        }

        return getGroupDetails(currentUsername, groupPublicId);
    }

    public void removeMember(String currentUsername, String groupPublicId, String targetUserPublicId) {
        ConversationMember adminMember = getActiveAdminMember(currentUsername, groupPublicId);
        Conversation conversation = adminMember.getConversation();
        User targetUser = getUserByPublicId(targetUserPublicId);

        if (adminMember.getUser().getId().equals(targetUser.getId())) {
            throw new BadRequestException("Admins cannot remove themselves; use leave group instead");
        }

        ConversationMember targetMember = conversationMemberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), targetUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", "publicId", targetUserPublicId));

        targetMember.setLeftAt(Instant.now());
        conversationMemberRepository.save(targetMember);

        String systemContent = adminMember.getUser().getFullName() + " removed " + targetUser.getFullName();
        messageService.sendSystemMessage(conversation, systemContent);
    }

    public void leaveGroup(String currentUsername, String groupPublicId) {
        User user = getUserByUsername(currentUsername);
        Conversation conversation = getGroupConversation(groupPublicId);

        ConversationMember membership = conversationMemberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of this group"));

        membership.setLeftAt(Instant.now());
        conversationMemberRepository.save(membership);

        // If leaving user was ADMIN, verify if other admins remain
        if (membership.getRole() == MemberRole.ADMIN) {
            long activeAdmins = conversationMemberRepository.countActiveAdminsByConversationId(conversation.getId());
            if (activeAdmins == 0) {
                // Auto-promote oldest remaining active member
                conversationMemberRepository.findFirstByConversationIdAndLeftAtIsNullOrderByJoinedAtAsc(conversation.getId())
                        .ifPresent(oldest -> {
                            oldest.setRole(MemberRole.ADMIN);
                            conversationMemberRepository.save(oldest);
                            String promoContent = oldest.getUser().getFullName() + " was automatically promoted to admin";
                            messageService.sendSystemMessage(conversation, promoContent);
                        });
            }
        }

        String systemContent = user.getFullName() + " left the group";
        messageService.sendSystemMessage(conversation, systemContent);
    }

    public GroupDetailsResponse updateMemberRole(String currentUsername, String groupPublicId, String targetUserPublicId, UpdateMemberRoleRequest request) {
        ConversationMember adminMember = getActiveAdminMember(currentUsername, groupPublicId);
        Conversation conversation = adminMember.getConversation();
        User targetUser = getUserByPublicId(targetUserPublicId);

        ConversationMember targetMember = conversationMemberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), targetUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", "publicId", targetUserPublicId));

        if (targetMember.getRole() != request.role()) {
            targetMember.setRole(request.role());
            conversationMemberRepository.save(targetMember);

            String systemContent = request.role() == MemberRole.ADMIN
                    ? adminMember.getUser().getFullName() + " made " + targetUser.getFullName() + " an admin"
                    : adminMember.getUser().getFullName() + " dismissed " + targetUser.getFullName() + " as admin";
            messageService.sendSystemMessage(conversation, systemContent);
        }

        return getGroupDetails(currentUsername, groupPublicId);
    }

    private ConversationMember getActiveAdminMember(String currentUsername, String groupPublicId) {
        User user = getUserByUsername(currentUsername);
        Conversation conversation = getGroupConversation(groupPublicId);

        ConversationMember member = conversationMemberRepository
                .findByConversationIdAndUserIdAndLeftAtIsNull(conversation.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not an active member of this group"));

        if (member.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Only group admins can perform this action");
        }

        return member;
    }

    private Conversation getGroupConversation(String publicId) {
        Conversation conversation = conversationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "publicId", publicId));

        if (conversation.getConversationType() != ConversationType.GROUP) {
            throw new BadRequestException("Conversation is not a group");
        }

        return conversation;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private User getUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", publicId));
    }
}
