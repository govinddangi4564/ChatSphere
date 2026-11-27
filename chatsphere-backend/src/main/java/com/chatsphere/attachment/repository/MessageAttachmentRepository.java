package com.chatsphere.attachment.repository;

import com.chatsphere.attachment.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    Optional<MessageAttachment> findByPublicId(String publicId);
    Optional<MessageAttachment> findByStoragePublicId(String storagePublicId);
    List<MessageAttachment> findAllByMessageId(Long messageId);
    List<MessageAttachment> findAllByPublicIdIn(List<String> publicIds);
}
