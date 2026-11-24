-- V3__user_management_schema.sql
-- ChatSphere Phase 3: User Management, Contacts, Blocking, and Privacy Schema

CREATE TABLE user_contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    contact_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_contacts UNIQUE (owner_id, contact_user_id),
    CONSTRAINT fk_user_contacts_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_contacts_contact FOREIGN KEY (contact_user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_contacts_owner (owner_id),
    INDEX idx_user_contacts_contact (contact_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blocked_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_blocked_users UNIQUE (blocker_id, blocked_user_id),
    CONSTRAINT fk_blocked_users_blocker FOREIGN KEY (blocker_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_users_blocked FOREIGN KEY (blocked_user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_blocked_users_blocker (blocker_id),
    INDEX idx_blocked_users_blocked (blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_privacy_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    profile_photo_visibility VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    last_seen_visibility VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    online_status_visibility VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    message_permission VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_privacy_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_privacy_settings_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
