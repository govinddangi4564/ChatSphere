package com.chatsphere.message.entity;

import com.chatsphere.common.enums.MessageType;
import com.chatsphere.conversation.entity.Conversation;
import com.chatsphere.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_messages_sender_client_msg", columnNames = {"sender_id", "client_message_id"})
}, indexes = {
        @Index(name = "idx_messages_conv_created_id", columnList = "conversation_id, created_at, id"),
        @Index(name = "idx_messages_public_id", columnList = "public_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "client_message_id", nullable = false, length = 64)
    private String clientMessageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @Column(name = "forwarded_from_message_id")
    private Long forwardedFromMessageId;

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "deleted_for_everyone", nullable = false)
    private boolean deletedForEveryone = false;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "pinned_at")
    private Instant pinnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by")
    private User pinnedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
