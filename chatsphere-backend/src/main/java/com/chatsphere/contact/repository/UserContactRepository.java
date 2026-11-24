package com.chatsphere.contact.repository;

import com.chatsphere.contact.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, Long> {

    @Query("SELECT c FROM UserContact c JOIN FETCH c.contactUser WHERE c.owner.id = :ownerId ORDER BY c.createdAt DESC")
    List<UserContact> findByOwnerIdWithContactUser(@Param("ownerId") Long ownerId);

    Optional<UserContact> findByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);

    boolean existsByOwnerIdAndContactUserId(Long ownerId, Long contactUserId);

    @Modifying
    @Query("DELETE FROM UserContact c WHERE c.owner.id = :ownerId AND c.contactUser.id = :contactUserId")
    void deleteByOwnerIdAndContactUserId(@Param("ownerId") Long ownerId, @Param("contactUserId") Long contactUserId);
}
