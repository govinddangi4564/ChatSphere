package com.chatsphere.conversation.repository;

import com.chatsphere.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByPublicId(String publicId);

    Optional<Conversation> findByPrivatePairHash(String privatePairHash);
}
