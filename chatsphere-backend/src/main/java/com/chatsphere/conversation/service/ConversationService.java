package com.chatsphere.conversation.service;

import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.conversation.dto.*;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.conversation.entity.ConversationMember;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.conversation.repository.ConversationRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationService {

    private static final Set<String> ALLOWED_SORTS = Set.of("lastMessageAt", "createdAt", "title");
    private static final int MAX_PAGE_SIZE = 50;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final UserContactRepository userContactRepository;
    private final UserPrivacySettingRepository privacySettingRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            UserRepository userRepository,
            BlockedUserRepository blockedUserRepository,
            UserContactRepository userContactRepository,
            UserPrivacySettingRepository privacySettingRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.userContactRepository = userContactRepository;
        this.privacySettingRepository = privacySettingRepository;
    }

    public ConversationDetailsResponse createOrFindPrivateConversation(String currentUsername, CreatePrivateConversationRequest request) {
        User currentUser = getUserByUsername(currentUsername);
        User recipientUser = getUserByPublicId(request.recipientPublicId());

        if (currentUser.getId().equals(recipientUser.getId())) {
            throw new BadRequestException("Cannot create a private conversation with yourself");
        }

        // Enforce block rules
        if (blockedUserRepository.isBidirectionallyBlocked(currentUser.getId(), recipientUser.getId())) {
            throw new ResourceNotFoundException("User", "publicId", request.recipientPublicId());
        }

        // Database-enforced pair hash for duplicate prevention
        long minId = Math.min(currentUser.getId(), recipientUser.getId());
        long maxId = Math.max(currentUser.getId(), recipientUser.getId());
        String pairHash = minId + "_" + maxId;

        Optional<Conversation> existing = conversationRepository.findByPrivatePairHash(pairHash);
        if (existing.isPresent()) {
            return getConversationDetails(currentUsername, existing.get().getPublicId());
        }

        // Create new private conversation
        Conversation conversation = new Conversation();
        conversation.setConversationType(ConversationType.PRIVATE);
        conversation.setPrivatePairHash(pairHash);
        conversation.setCreatedBy(currentUser);
        Conversation savedConversation = conversationRepository.save(conversation);

        // Add both members
        ConversationMember member1 = new ConversationMember();
        member1.setConversation(savedConversation);
        member1.setUser(currentUser);
        member1.setRole(MemberRole.MEMBER);
        conversationMemberRepository.save(member1);

        ConversationMember member2 = new ConversationMember();
        member2.setConversation(savedConversation);
        member2.setUser(recipientUser);
        member2.setRole(MemberRole.MEMBER);
        conversationMemberRepository.save(member2);

        return getConversationDetails(currentUsername, savedConversation.getPublicId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> getConversations(
            String currentUsername,
            ConversationType type,
            Boolean archived,
            Boolean muted,
            Boolean unread,
            Pageable pageable
    ) {
        User currentUser = getUserByUsername(currentUsername);
        Pageable safePageable = validateAndBuildPageable(pageable);

        Page<ConversationMember> membershipPage = conversationMemberRepository.findUserMemberships(
                currentUser.getId(),
                type,
                archived,
                muted,
                unread,
                safePageable
        );

        if (membershipPage.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), membershipPage);
        }

        // Avoid N+1: batch-fetch all members for these conversations in one query
        List<Long> conversationIds = membershipPage.getContent().stream()
                .map(cm -> cm.getConversation().getId())
                .toList();

        List<ConversationMember> allMembers = conversationMemberRepository.findAllByConversationIdIn(conversationIds);
        Map<Long, List<ConversationMember>> membersByConvId = allMembers.stream()
                .collect(Collectors.groupingBy(cm -> cm.getConversation().getId()));

        List<ConversationSummaryResponse> summaries = membershipPage.getContent().stream()
                .map(cm -> {
                    Conversation conv = cm.getConversation();
                    List<ConversationMember> members = membersByConvId.getOrDefault(conv.getId(), Collections.emptyList());

                    ConversationParticipantResponse partnerDto = null;
                    String computedTitle = conv.getTitle();
                    String computedImage = conv.getImageUrl();

                    if (conv.getConversationType() == ConversationType.PRIVATE) {
                        ConversationMember partnerMember = members.stream()
                                .filter(m -> !m.getUser().getId().equals(currentUser.getId()))
                                .findFirst()
                                .orElse(null);

                        if (partnerMember != null) {
                            User partner = partnerMember.getUser();
                            partnerDto = buildParticipantDto(currentUser.getId(), partner, partnerMember.getRole().name());
                            computedTitle = partner.getFullName();
                            computedImage = partnerDto.profileImageUrl();
                        }
                    }

                    return new ConversationSummaryResponse(
                            conv.getPublicId(),
                            conv.getConversationType(),
                            computedTitle,
                            computedImage,
                            conv.getLastMessageAt(),
                            conv.getLastMessagePreview(),
                            cm.getUnreadCount(),
                            cm.isMuted(),
                            cm.isArchived(),
                            partnerDto
                    );
                })
                .toList();

        return PageResponse.of(summaries, membershipPage);
    }

    @Transactional(readOnly = true)
    public ConversationDetailsResponse getConversationDetails(String currentUsername, String publicId) {
        User currentUser = getUserByUsername(currentUsername);

        ConversationMember membership = conversationMemberRepository.findByConversationPublicIdAndUserId(publicId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", publicId));

        Conversation conversation = membership.getConversation();
        List<ConversationMember> allMembers = conversationMemberRepository.findAllByConversationId(conversation.getId());

        List<ConversationParticipantResponse> participants = allMembers.stream()
                .map(m -> buildParticipantDto(currentUser.getId(), m.getUser(), m.getRole().name()))
                .toList();

        String computedTitle = conversation.getTitle();
        String computedImage = conversation.getImageUrl();

        if (conversation.getConversationType() == ConversationType.PRIVATE) {
            ConversationParticipantResponse partner = participants.stream()
                    .filter(p -> !p.publicId().equals(currentUser.getPublicId()))
                    .findFirst()
                    .orElse(null);
            if (partner != null) {
                computedTitle = partner.fullName();
                computedImage = partner.profileImageUrl();
            }
        }

        return new ConversationDetailsResponse(
                conversation.getPublicId(),
                conversation.getConversationType(),
                computedTitle,
                computedImage,
                conversation.getCreatedAt(),
                conversation.getLastMessageAt(),
                participants,
                membership.isMuted(),
                membership.isArchived(),
                membership.getUnreadCount(),
                membership.getRole().name()
        );
    }

    public void archiveConversation(String currentUsername, String publicId, boolean archived) {
        User currentUser = getUserByUsername(currentUsername);
        ConversationMember membership = conversationMemberRepository.findByConversationPublicIdAndUserId(publicId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", publicId));

        membership.setArchived(archived);
        conversationMemberRepository.save(membership);
    }

    public void muteConversation(String currentUsername, String publicId, boolean muted) {
        User currentUser = getUserByUsername(currentUsername);
        ConversationMember membership = conversationMemberRepository.findByConversationPublicIdAndUserId(publicId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "publicId", publicId));

        membership.setMuted(muted);
        conversationMemberRepository.save(membership);
    }

    private ConversationParticipantResponse buildParticipantDto(Long currentUserId, User targetUser, String role) {
        boolean isContact = userContactRepository.existsByOwnerIdAndContactUserId(targetUser.getId(), currentUserId);
        UserPrivacySetting privacy = privacySettingRepository.findByUserId(targetUser.getId()).orElse(null);

        boolean canViewPhoto = canAccess(privacy != null ? privacy.getProfilePhotoVisibility() : PrivacyVisibility.EVERYONE, isContact);
        boolean canViewLastSeen = canAccess(privacy != null ? privacy.getLastSeenVisibility() : PrivacyVisibility.EVERYONE, isContact);

        return new ConversationParticipantResponse(
                targetUser.getPublicId(),
                targetUser.getFullName(),
                targetUser.getUsername(),
                canViewPhoto ? targetUser.getProfileImageUrl() : null,
                canViewLastSeen ? targetUser.getLastSeenAt() : null,
                role
        );
    }

    private boolean canAccess(PrivacyVisibility visibility, boolean isContact) {
        if (visibility == null || visibility == PrivacyVisibility.EVERYONE) {
            return true;
        }
        if (visibility == PrivacyVisibility.CONTACTS_ONLY) {
            return isContact;
        }
        return false;
    }

    private Pageable validateAndBuildPageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }

        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, pageable.getPageSize());

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "c.lastMessageAt", "c.createdAt"));
        }

        List<Sort.Order> safeOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String prop = order.getProperty();
            if (!ALLOWED_SORTS.contains(prop)) {
                throw new BadRequestException("Invalid sort field: '" + prop + "'. Permitted sort fields are: " + ALLOWED_SORTS);
            }
            safeOrders.add(new Sort.Order(order.getDirection(), "c." + prop));
        }

        return PageRequest.of(page, size, Sort.by(safeOrders));
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
