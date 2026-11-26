package com.chatsphere.message.repository;

import com.chatsphere.message.entity.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

    Optional<MessageReceipt> findByMessageIdAndRecipientId(Long messageId, Long recipientId);

    List<MessageReceipt> findAllByMessageId(Long messageId);

    @Query("SELECT r FROM MessageReceipt r " +
           "JOIN FETCH r.message m " +
           "JOIN FETCH r.recipient u " +
           "WHERE m.publicId = :messagePublicId AND u.id = :recipientId")
    Optional<MessageReceipt> findByMessagePublicIdAndRecipientId(
            @Param("messagePublicId") String messagePublicId,
            @Param("recipientId") Long recipientId
    );

    @Query("SELECT r FROM MessageReceipt r " +
           "JOIN FETCH r.message m " +
           "WHERE m.conversation.id = :conversationId AND r.recipient.id = :recipientId AND r.readAt IS NULL")
    List<MessageReceipt> findUnreadReceiptsForConversation(
            @Param("conversationId") Long conversationId,
            @Param("recipientId") Long recipientId
    );
}
