package com.chatsphere.message.repository;

import com.chatsphere.message.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);
    List<MessageReaction> findAllByMessageId(Long messageId);
    List<MessageReaction> findAllByMessageIdIn(List<Long> messageIds);
    void deleteByMessageIdAndUserId(Long messageId, Long userId);
}
