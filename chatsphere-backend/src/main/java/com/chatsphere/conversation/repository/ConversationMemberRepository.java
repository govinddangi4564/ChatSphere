package com.chatsphere.conversation.repository;

import com.chatsphere.common.enums.ConversationType;
import com.chatsphere.conversation.entity.ConversationMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT cm FROM ConversationMember cm " +
           "JOIN FETCH cm.conversation c " +
           "WHERE c.publicId = :publicId AND cm.user.id = :userId")
    Optional<ConversationMember> findByConversationPublicIdAndUserId(
            @Param("publicId") String publicId,
            @Param("userId") Long userId
    );

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT cm FROM ConversationMember cm " +
           "JOIN cm.conversation c " +
           "WHERE cm.user.id = :userId " +
           "AND (:type IS NULL OR c.conversationType = :type) " +
           "AND (:archived IS NULL OR cm.archived = :archived) " +
           "AND (:muted IS NULL OR cm.muted = :muted) " +
           "AND (:unread IS NULL OR (:unread = true AND cm.unreadCount > 0) OR (:unread = false AND cm.unreadCount = 0))")
    Page<ConversationMember> findUserMemberships(
            @Param("userId") Long userId,
            @Param("type") ConversationType type,
            @Param("archived") Boolean archived,
            @Param("muted") Boolean muted,
            @Param("unread") Boolean unread,
            Pageable pageable
    );

    @Query("SELECT cm FROM ConversationMember cm " +
           "JOIN FETCH cm.user u " +
           "WHERE cm.conversation.id IN :conversationIds")
    List<ConversationMember> findAllByConversationIdIn(@Param("conversationIds") Collection<Long> conversationIds);

    @Query("SELECT cm FROM ConversationMember cm " +
           "JOIN FETCH cm.user u " +
           "WHERE cm.conversation.id = :conversationId")
    List<ConversationMember> findAllByConversationId(@Param("conversationId") Long conversationId);
}
