package com.chatsphere.user.repository;

import com.chatsphere.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByPublicId(String publicId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id != :currentUserId " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("currentUserId") Long currentUserId,
                           @Param("query") String query,
                           Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id != :currentUserId " +
           "AND u.id NOT IN :excludedUserIds " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsersExcluding(@Param("currentUserId") Long currentUserId,
                                    @Param("excludedUserIds") Collection<Long> excludedUserIds,
                                    @Param("query") String query,
                                    Pageable pageable);
}
