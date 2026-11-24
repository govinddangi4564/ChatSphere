package com.chatsphere.user.entity;

import com.chatsphere.common.enums.MessagePermission;
import com.chatsphere.common.enums.PrivacyVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_privacy_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserPrivacySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_photo_visibility", nullable = false, length = 20)
    private PrivacyVisibility profilePhotoVisibility = PrivacyVisibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_seen_visibility", nullable = false, length = 20)
    private PrivacyVisibility lastSeenVisibility = PrivacyVisibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status_visibility", nullable = false, length = 20)
    private PrivacyVisibility onlineStatusVisibility = PrivacyVisibility.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_permission", nullable = false, length = 20)
    private MessagePermission messagePermission = MessagePermission.EVERYONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
