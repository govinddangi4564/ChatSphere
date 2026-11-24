package com.chatsphere.contact.service;

import com.chatsphere.common.enums.PrivacyVisibility;
import com.chatsphere.contact.dto.ContactResponse;
import com.chatsphere.contact.entity.UserContact;
import com.chatsphere.contact.repository.UserContactRepository;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ConflictException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.entity.UserPrivacySetting;
import com.chatsphere.user.mapper.UserMapper;
import com.chatsphere.user.repository.BlockedUserRepository;
import com.chatsphere.user.repository.UserPrivacySettingRepository;
import com.chatsphere.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactService {

    private final UserRepository userRepository;
    private final UserContactRepository userContactRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final UserPrivacySettingRepository privacySettingRepository;

    public ContactService(
            UserRepository userRepository,
            UserContactRepository userContactRepository,
            BlockedUserRepository blockedUserRepository,
            UserPrivacySettingRepository privacySettingRepository
    ) {
        this.userRepository = userRepository;
        this.userContactRepository = userContactRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.privacySettingRepository = privacySettingRepository;
    }

    public ContactResponse addContact(String currentUsername, String targetPublicId) {
        User currentUser = getUserByUsername(currentUsername);
        User targetUser = getUserByPublicId(targetPublicId);

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("You cannot add yourself as a contact");
        }

        // Check if caller is blocked by target
        if (blockedUserRepository.existsByBlockerIdAndBlockedUserId(targetUser.getId(), currentUser.getId())) {
            throw new ResourceNotFoundException("User", "publicId", targetPublicId);
        }

        if (userContactRepository.existsByOwnerIdAndContactUserId(currentUser.getId(), targetUser.getId())) {
            throw new ConflictException("User is already in your contacts");
        }

        UserContact contact = new UserContact();
        contact.setOwner(currentUser);
        contact.setContactUser(targetUser);
        UserContact savedContact = userContactRepository.save(contact);

        boolean isTargetContactOfCurrent = userContactRepository.existsByOwnerIdAndContactUserId(targetUser.getId(), currentUser.getId());
        UserPrivacySetting privacy = privacySettingRepository.findByUserId(targetUser.getId()).orElse(null);

        boolean canViewPhoto = canAccess(privacy != null ? privacy.getProfilePhotoVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);
        boolean canViewLastSeen = canAccess(privacy != null ? privacy.getLastSeenVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);

        return UserMapper.toContactResponse(savedContact, canViewPhoto, canViewLastSeen);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(String currentUsername) {
        User currentUser = getUserByUsername(currentUsername);
        List<UserContact> contacts = userContactRepository.findByOwnerIdWithContactUser(currentUser.getId());

        return contacts.stream()
                .map(contact -> {
                    User target = contact.getContactUser();
                    boolean isTargetContactOfCurrent = userContactRepository.existsByOwnerIdAndContactUserId(target.getId(), currentUser.getId());
                    UserPrivacySetting privacy = privacySettingRepository.findByUserId(target.getId()).orElse(null);

                    boolean canViewPhoto = canAccess(privacy != null ? privacy.getProfilePhotoVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);
                    boolean canViewLastSeen = canAccess(privacy != null ? privacy.getLastSeenVisibility() : PrivacyVisibility.EVERYONE, isTargetContactOfCurrent);

                    return UserMapper.toContactResponse(contact, canViewPhoto, canViewLastSeen);
                })
                .toList();
    }

    public void removeContact(String currentUsername, String targetPublicId) {
        User currentUser = getUserByUsername(currentUsername);
        User targetUser = getUserByPublicId(targetPublicId);

        userContactRepository.deleteByOwnerIdAndContactUserId(currentUser.getId(), targetUser.getId());
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
}
