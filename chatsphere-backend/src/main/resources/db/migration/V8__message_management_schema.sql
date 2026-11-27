-- V8__message_management_schema.sql
-- ChatSphere Phase 8: Message Management (Reactions, Deletions, Pins, Replies, Cleared Conversation)

-- 1. Message reactions
CREATE TABLE IF NOT EXISTS message_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_message_user_reaction UNIQUE (message_id, user_id),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reactions_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Message deletions (Delete for Me)
CREATE TABLE IF NOT EXISTS message_deletions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_message_user_deletion UNIQUE (message_id, user_id),
    CONSTRAINT fk_deletion_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_deletion_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_deletions_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Add pinned fields to messages
ALTER TABLE messages 
    ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pinned_at TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN pinned_by BIGINT NULL DEFAULT NULL,
    ADD CONSTRAINT fk_messages_pinned_by FOREIGN KEY (pinned_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD INDEX idx_messages_pinned (conversation_id, is_pinned);

-- 4. Add cleared_at to conversation_members (for "delete conversation for me")
ALTER TABLE conversation_members
    ADD COLUMN cleared_at TIMESTAMP NULL DEFAULT NULL;
