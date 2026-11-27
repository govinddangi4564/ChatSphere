package com.chatsphere.message.repository;

import com.chatsphere.message.entity.Message;
import org.springframework.data.domain.Page;
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
           "AND (:clearedAt IS NULL OR m.createdAt > :clearedAt) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findNewestMessages(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") Instant clearedAt,
            Pageable pageable
    );

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND (:clearedAt IS NULL OR m.createdAt > :clearedAt) " +
           "AND (m.createdAt < :cursorCreatedAt OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<Message> findMessagesBeforeCursor(
            @Param("conversationId") Long conversationId,
            @Param("clearedAt") Instant clearedAt,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.isPinned = true " +
           "ORDER BY m.pinnedAt DESC")
    List<Message> findPinnedMessages(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender JOIN FETCH m.conversation " +
           "WHERE m.conversation.id IN :conversationIds " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND (:startDate IS NULL OR m.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR m.createdAt <= :endDate) " +
           "AND m.deletedForEveryone = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchMessages(
            @Param("conversationIds") List<Long> conversationIds,
            @Param("query") String query,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
