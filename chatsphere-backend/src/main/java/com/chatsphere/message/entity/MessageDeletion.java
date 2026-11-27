package com.chatsphere.message.entity;

import com.chatsphere.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "message_deletions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_user_deletion", columnNames = {"message_id", "user_id"})
}, indexes = {
        @Index(name = "idx_deletions_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;
}
