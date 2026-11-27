package com.chatsphere.attachment.service;

import com.chatsphere.attachment.dto.AttachmentResponse;
import com.chatsphere.attachment.entity.MessageAttachment;
import com.chatsphere.attachment.repository.MessageAttachmentRepository;
import com.chatsphere.common.enums.MemberRole;
import com.chatsphere.conversation.repository.ConversationMemberRepository;
import com.chatsphere.exception.AppException;
import com.chatsphere.exception.BadRequestException;
import com.chatsphere.exception.ResourceNotFoundException;
import com.chatsphere.message.entity.Message;
import com.chatsphere.user.entity.User;
import com.chatsphere.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;

    public AttachmentService(
            FileStorageService fileStorageService,
            FileValidationService fileValidationService,
            MessageAttachmentRepository attachmentRepository,
            ConversationMemberRepository conversationMemberRepository,
            UserRepository userRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
        this.attachmentRepository = attachmentRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AttachmentResponse uploadAttachment(MultipartFile file, String currentUsername) {
        User user = getUserByUsername(currentUsername);
        FileValidationService.ValidatedFileInfo fileInfo = fileValidationService.validate(file);

        StoredFile storedFile = fileStorageService.upload(file, "chatsphere/attachments");

        try {
            MessageAttachment attachment = MessageAttachment.builder()
                    .publicId(UUID.randomUUID().toString())
                    .storagePublicId(storedFile.storagePublicId())
                    .fileName(fileInfo.originalFileName())
                    .fileType(fileInfo.fileType())
                    .mimeType(fileInfo.detectedMimeType())
                    .fileSize(fileInfo.fileSize())
                    .storageUrl(storedFile.storageUrl())
                    .thumbnailUrl(storedFile.thumbnailUrl())
                    .build();

            MessageAttachment saved = attachmentRepository.save(attachment);
            return toResponse(saved);
        } catch (Exception e) {
            log.error("Failed to save attachment metadata to database; cleaning up stored file: {}", storedFile.storagePublicId(), e);
            fileStorageService.delete(storedFile.storagePublicId());
            throw new AppException("Failed to persist attachment metadata");
        }
    }

    @Transactional(readOnly = true)
    public AttachmentResourceInfo getAttachmentForDownload(String publicId, String currentUsername) {
        User user = getUserByUsername(currentUsername);
        MessageAttachment attachment = attachmentRepository.findByPublicId(publicId)
                .orElseGet(() -> attachmentRepository.findByStoragePublicId(publicId)
                        .orElseThrow(() -> new ResourceNotFoundException("Attachment", "publicId", publicId)));

        // Authorization check: if the attachment is linked to a message, user must be an active participant
        if (attachment.getMessage() != null) {
            Long conversationId = attachment.getMessage().getConversation().getId();
            boolean isMember = conversationMemberRepository.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, user.getId()).isPresent();
            if (!isMember) {
                throw new AccessDeniedException("You do not have permission to access this attachment");
            }
        }

        Resource resource = null;
        if (fileStorageService instanceof LocalFileStorageService localStorageService) {
            resource = localStorageService.loadAsResource(attachment.getStoragePublicId());
        }

        return new AttachmentResourceInfo(
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getStorageUrl(),
                resource
        );
    }

    @Transactional
    public void deleteAttachment(String publicId, String currentUsername) {
        User user = getUserByUsername(currentUsername);
        MessageAttachment attachment = attachmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "publicId", publicId));

        if (attachment.getMessage() != null) {
            Message message = attachment.getMessage();
            boolean isSender = message.getSender().getId().equals(user.getId());
            boolean isAdmin = conversationMemberRepository
                    .findByConversationIdAndUserId(message.getConversation().getId(), user.getId())
                    .map(m -> m.getRole() == MemberRole.ADMIN && m.getLeftAt() == null)
                    .orElse(false);

            if (!isSender && !isAdmin) {
                throw new AccessDeniedException("Only the message sender or a group admin can delete attachments");
            }
        }

        fileStorageService.delete(attachment.getStoragePublicId());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void linkAttachmentsToMessage(Message message, List<String> attachmentPublicIds) {
        if (attachmentPublicIds == null || attachmentPublicIds.isEmpty()) {
            return;
        }

        List<MessageAttachment> attachments = attachmentRepository.findAllByPublicIdIn(attachmentPublicIds);
        for (MessageAttachment att : attachments) {
            if (att.getMessage() == null) {
                att.setMessage(message);
                attachmentRepository.save(att);
            }
        }
    }

    public AttachmentResponse toResponse(MessageAttachment att) {
        return new AttachmentResponse(
                att.getPublicId(),
                att.getFileName(),
                att.getFileType(),
                att.getMimeType(),
                att.getFileSize(),
                att.getStorageUrl(),
                att.getThumbnailUrl(),
                att.getCreatedAt()
        );
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public record AttachmentResourceInfo(
            String fileName,
            String mimeType,
            Long fileSize,
            String storageUrl,
            Resource resource
    ) {}
}
