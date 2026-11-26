package com.chatsphere.message.repository;

import com.chatsphere.message.entity.MessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

    Optional<MessageReceipt> findByMessageIdAndRecipientId(Long messageId, Long recipientId);

    List<MessageReceipt> findAllByMessageId(Long messageId);
}
