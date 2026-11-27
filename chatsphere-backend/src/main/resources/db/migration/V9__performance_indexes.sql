-- V9__performance_indexes.sql
-- ChatSphere Phase 9: Performance Optimization & Justified Indexes

-- 1. Optimized composite index for user conversation retrieval with cleared_at filtering
ALTER TABLE conversation_members
    ADD INDEX idx_conv_members_user_cleared (user_id, cleared_at);

-- 2. Covering composite index for high-volume message search and keyset pagination
ALTER TABLE messages
    ADD INDEX idx_messages_search_covering (conversation_id, deleted_for_everyone, created_at, id);
