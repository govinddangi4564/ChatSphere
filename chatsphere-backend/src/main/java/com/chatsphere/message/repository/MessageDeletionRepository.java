package com.chatsphere.message.repository;

import com.chatsphere.message.entity.MessageDeletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageDeletionRepository extends JpaRepository<MessageDeletion, Long> {
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
    List<MessageDeletion> findAllByUserIdAndMessageIdIn(Long userId, List<Long> messageIds);
    List<MessageDeletion> findAllByUserId(Long userId);
}
