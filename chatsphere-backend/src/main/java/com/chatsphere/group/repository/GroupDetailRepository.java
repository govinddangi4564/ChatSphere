package com.chatsphere.group.repository;

import com.chatsphere.group.entity.GroupDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupDetailRepository extends JpaRepository<GroupDetail, Long> {

    Optional<GroupDetail> findByConversationId(Long conversationId);

    @Query("SELECT gd FROM GroupDetail gd JOIN FETCH gd.conversation c WHERE c.publicId = :publicId")
    Optional<GroupDetail> findByConversationPublicId(@Param("publicId") String publicId);
}
