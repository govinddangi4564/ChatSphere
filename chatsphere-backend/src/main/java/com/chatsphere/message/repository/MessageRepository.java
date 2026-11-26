package com.chatsphere.message.repository;

import com.chatsphere.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    Optional<Message> findByPublicId(String publicId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findNewestMessages(
            @Param("conversationId") Long conversationId,
            Pageable pageable
    );

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND (m.createdAt < :cursorCreatedAt OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findMessagesBeforeCursor(
            @Param("conversationId") Long conversationId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
