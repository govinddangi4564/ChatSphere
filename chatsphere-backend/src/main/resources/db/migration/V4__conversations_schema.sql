-- V4__conversations_schema.sql
-- ChatSphere Phase 4: Conversations and Conversation Members Schema

CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
    conversation_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    title VARCHAR(100) NULL,
    image_url VARCHAR(255) NULL,
    private_pair_hash VARCHAR(50) NULL UNIQUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_conversations_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_conversations_public_id (public_id),
    INDEX idx_conversations_pair_hash (private_pair_hash),
    INDEX idx_conversations_last_msg (last_message_at),
    INDEX idx_conversations_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversation_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP NULL DEFAULT NULL,
    muted BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    last_read_message_id BIGINT NULL DEFAULT NULL,
    unread_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_conversation_member UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_conversation_members_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_conv_members_user (user_id),
    INDEX idx_conv_members_conversation (conversation_id),
    INDEX idx_conv_members_user_archived (user_id, archived),
    INDEX idx_conv_members_user_muted (user_id, muted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
