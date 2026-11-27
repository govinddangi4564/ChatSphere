package com.chatsphere.user.service;

import com.chatsphere.common.dto.PageResponse;
import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ConflictException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.dto.*;
import com.chatsphere.user.entity.BlockedUser;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;
import com.chatsphere.user.mapper.UserMapper;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class UserService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("username", "fullName");

    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final UserContactRepository userContactRepository;
    private final UserPrivacySettingRepository privacySettingRepository;
    private final com.chatsphere.cache.service.RedisCacheService redisCacheService;

    public UserService(
            UserRepository userRepository,
            BlockedUserRepository blockedUserRepository,
            UserContactRepository userContactRepository,
            UserPrivacySettingRepository privacySettingRepository,
            com.chatsphere.cache.service.RedisCacheService redisCacheService
    ) {
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.userContactRepository = userContactRepository;
        this.privacySettingRepository = privacySettingRepository;
        this.redisCacheService = redisCacheService;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(String username) {
        String key = "user:profile:username:" + username;
        return redisCacheService.getOrCompute(
                key,
                java.time.Duration.ofMinutes(10),
                UserResponse.class,
                () -> UserMapper.toUserResponse(getUserByUsername(username))
        );
    }

    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = getUserByUsername(username);
        user.setFullName(request.fullName().trim());
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }
        if (request.profileImageUrl() != null) {
            user.setProfileImageUrl(request.profileImageUrl().trim());
        }
        User saved = userRepository.save(user);

        // Invalidate profile cache
        redisCacheService.evict("user:profile:username:" + username);
        redisCacheService.evict("user:profile:publicId:" + user.getPublicId());

        return UserMapper.toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserPublicProfileResponse getUserProfile(String currentUsername, String publicId) {
        User currentUser = getUserByUsername(currentUsername);
        User targetUser = getUserByPublicId(publicId);

        boolean isBlockedByTarget = blockedUserRepository.existsByBlockerIdAndBlockedUserId(targetUser.getId(), currentUser.getId());
        if (isBlockedByTarget) {
            throw new ResourceNotFoundException("User", "publicId", publicId);
        }

        boolean isBlocked = blockedUserRepository.existsByBlockerIdAndBlockedUserId(currentUser.getId(), targetUser.getId());
        boolean isContact = userContactRepository.existsByOwnerIdAndContactUserId(currentUser.getId(), targetUser.getId());
        boolean isTargetContactOfCurrent = userContactRepository.existsByOwnerIdAndContactUserId(targetUser.getId(), currentUser.getId());

        UserPrivacySetting privacy = privacySettingRepository.findByUserId(targetUser.getId())
                .orElse(null);

        boolean canViewPhoto = canAccess(privacy != null ? privacy.getProfilePhotoVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);
        boolean canViewLastSeen = canAccess(privacy != null ? privacy.getLastSeenVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);

        return UserMapper.toUserPublicProfileResponse(targetUser, canViewPhoto, canViewLastSeen, isContact, isBlocked);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserPublicProfileResponse> searchUsers(String currentUsername, String query, Pageable pageable) {
        User currentUser = getUserByUsername(currentUsername);

        validateSort(pageable.getSort());

        Pageable safePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "username"));
        }

        String escapedQuery = escapeSqlWildcards(query);

        Set<Long> excludedUserIds = new HashSet<>();
        excludedUserIds.addAll(blockedUserRepository.findBlockedUserIdsByBlockerId(currentUser.getId()));
        excludedUserIds.addAll(blockedUserRepository.findBlockerIdsByBlockedUserId(currentUser.getId()));

        Page<User> usersPage;
        if (excludedUserIds.isEmpty()) {
            usersPage = userRepository.searchUsers(currentUser.getId(), escapedQuery, safePageable);
        } else {
            usersPage = userRepository.searchUsersExcluding(currentUser.getId(), excludedUserIds, escapedQuery, safePageable);
        }

        List<UserPublicProfileResponse> content = usersPage.getContent().stream()
                .map(target -> {
                    boolean isContact = userContactRepository.existsByOwnerIdAndContactUserId(currentUser.getId(), target.getId());
                    boolean isTargetContact = userContactRepository.existsByOwnerIdAndContactUserId(target.getId(), currentUser.getId());
                    UserPrivacySetting privacy = privacySettingRepository.findByUserId(target.getId()).orElse(null);

                    boolean canViewPhoto = canAccess(privacy != null ? privacy.getProfilePhotoVisibility() : PrivacyVisibility.EVERYONE, isTargetContact);
                    boolean canViewLastSeen = canAccess(privacy != null ? privacy.getLastSeenVisibility() : PrivacyVisibility.EVERYONE, isTargetContact);

                    return UserMapper.toUserPublicProfileResponse(target, canViewPhoto, canViewLastSeen, isContact, false);
                })
                .toList();

        return PageResponse.of(content, usersPage);
    }

    public void blockUser(String currentUsername, String targetPublicId) {
        User currentUser = getUserByUsername(currentUsername);
        User targetUser = getUserByPublicId(targetPublicId);

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("You cannot block yourself");
        }

        if (blockedUserRepository.existsByBlockerIdAndBlockedUserId(currentUser.getId(), targetUser.getId())) {
            return;
        }

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlocker(currentUser);
        blockedUser.setBlockedUser(targetUser);
        blockedUserRepository.save(blockedUser);
    }

    public void unblockUser(String currentUsername, String targetPublicId) {
        User currentUser = getUserByUsername(currentUsername);
        User targetUser = getUserByPublicId(targetPublicId);

        blockedUserRepository.deleteByBlockerIdAndBlockedUserId(currentUser.getId(), targetUser.getId());
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(String currentUsername) {
        User currentUser = getUserByUsername(currentUsername);
        List<BlockedUser> blockedUsers = blockedUserRepository.findByBlockerIdWithBlockedUser(currentUser.getId());
        return blockedUsers.stream()
                .map(UserMapper::toBlockedUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivacySettingsResponse getPrivacySettings(String currentUsername) {
        User currentUser = getUserByUsername(currentUsername);
        return privacySettingRepository.findByUserId(currentUser.getId())
                .map(UserMapper::toPrivacySettingsResponse)
                .orElseGet(() -> new PrivacySettingsResponse(
                        PrivacyVisibility.EVERYONE,
                        PrivacyVisibility.EVERYONE,
                        PrivacyVisibility.EVERYONE,
                        MessagePermission.EVERYONE
                ));
    }

    public PrivacySettingsResponse updatePrivacySettings(String currentUsername, PrivacySettingsRequest request) {
        User currentUser = getUserByUsername(currentUsername);
        UserPrivacySetting setting = privacySettingRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    UserPrivacySetting newSetting = new UserPrivacySetting();
                    newSetting.setUser(currentUser);
                    return newSetting;
                });

        setting.setProfilePhotoVisibility(request.profilePhotoVisibility());
        setting.setLastSeenVisibility(request.lastSeenVisibility());
        setting.setOnlineStatusVisibility(request.onlineStatusVisibility());
        setting.setMessagePermission(request.messagePermission());

        UserPrivacySetting saved = privacySettingRepository.save(setting);
        return UserMapper.toPrivacySettingsResponse(saved);
    }

    public void updateLastSeen(String currentUsername) {
        User user = getUserByUsername(currentUsername);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private User getUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", publicId));
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

    private void validateSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new BadRequestException("Invalid sort field: '" + order.getProperty() + "'. Permitted sort fields are: " + ALLOWED_SORT_PROPERTIES);
            }
        }
    }

    public String escapeSqlWildcards(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .trim();
    }
}
