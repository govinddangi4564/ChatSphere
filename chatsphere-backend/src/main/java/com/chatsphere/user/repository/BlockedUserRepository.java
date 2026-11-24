package com.chatsphere.user.repository;

import com.chatsphere.user.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blockedUser WHERE b.blocker.id = :blockerId ORDER BY b.createdAt DESC")
    List<BlockedUser> findByBlockerIdWithBlockedUser(@Param("blockerId") Long blockerId);

    Optional<BlockedUser> findByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);

    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BlockedUser b " +
           "WHERE (b.blocker.id = :user1Id AND b.blockedUser.id = :user2Id) " +
           "   OR (b.blocker.id = :user2Id AND b.blockedUser.id = :user1Id)")
    boolean isBidirectionallyBlocked(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT b.blockedUser.id FROM BlockedUser b WHERE b.blocker.id = :userId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM BlockedUser b WHERE b.blockedUser.id = :userId")
    List<Long> findBlockerIdsByBlockedUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM BlockedUser b WHERE b.blocker.id = :blockerId AND b.blockedUser.id = :blockedUserId")
    void deleteByBlockerIdAndBlockedUserId(@Param("blockerId") Long blockerId, @Param("blockedUserId") Long blockedUserId);
}
