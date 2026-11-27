# ChatSphere Database Design & Entity Relationships

## 1. Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : "has"
    USERS ||--o{ EMAIL_VERIFICATION_TOKENS : "receives"
    USERS ||--o{ PASSWORD_RESET_TOKENS : "receives"
    USERS ||--o{ USER_PRIVACY_SETTINGS : "configures"
    USERS ||--o{ USER_CONTACTS : "owns"
    USERS ||--o{ BLOCKED_USERS : "blocks"
    USERS ||--o{ CONVERSATION_MEMBERS : "participates_in"
    USERS ||--o{ MESSAGES : "sends"
    USERS ||--o{ MESSAGE_RECEIPTS : "receives"
    USERS ||--o{ MESSAGE_REACTIONS : "reacts"
    USERS ||--o{ MESSAGE_DELETIONS : "deletes_for_me"
    USERS ||--o{ NOTIFICATIONS : "receives"

    CONVERSATIONS ||--o{ CONVERSATION_MEMBERS : "contains"
    CONVERSATIONS ||--o| GROUP_DETAILS : "has_details"
    CONVERSATIONS ||--o{ MESSAGES : "holds"

    MESSAGES ||--o{ MESSAGE_RECEIPTS : "tracked_by"
    MESSAGES ||--o{ MESSAGE_ATTACHMENTS : "includes"
    MESSAGES ||--o{ MESSAGE_REACTIONS : "receives"
    MESSAGES ||--o{ MESSAGE_DELETIONS : "marked_by"

    USERS {
        bigint id PK
        varchar public_id UK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar profile_image_url
        varchar account_status
        boolean email_verified
        timestamp last_seen_at
        timestamp created_at
    }

    CONVERSATIONS {
        bigint id PK
        varchar public_id UK
        varchar conversation_type
        varchar title
        varchar image_url
        bigint created_by FK
        varchar private_pair_hash UK
        timestamp last_message_at
        text last_message_preview
        timestamp created_at
    }

    CONVERSATION_MEMBERS {
        bigint id PK
        bigint conversation_id FK
        bigint user_id FK
        varchar role
        boolean is_muted
        boolean is_archived
        int unread_count
        timestamp joined_at
        timestamp left_at
        timestamp cleared_at
    }

    MESSAGES {
        bigint id PK
        varchar public_id UK
        varchar client_message_id
        bigint conversation_id FK
        bigint sender_id FK
        varchar message_type
        text content
        bigint reply_to_message_id FK
        boolean is_pinned
        boolean is_edited
        boolean deleted_for_everyone
        timestamp created_at
    }

    NOTIFICATIONS {
        bigint id PK
        varchar public_id UK
        bigint user_id FK
        varchar notification_type
        varchar title
        text message
        varchar reference_id
        boolean is_read
        timestamp created_at
    }
```

---

## 2. Table Indexing & Performance Design

- **`messages`**:
  - `idx_messages_cursor (conversation_id, created_at, id)`: Powers high-performance cursor/keyset pagination.
  - `idx_messages_search_covering (conversation_id, deleted_for_everyone, created_at, id)`: Covering index for multi-conversation full text filtering.
  - `idx_messages_pinned (conversation_id, is_pinned)`: Fast retrieval of pinned announcements in chats.
- **`conversation_members`**:
  - `uk_conversation_user (conversation_id, user_id)`: Unique active membership constraint.
  - `idx_conv_members_user_cleared (user_id, cleared_at)`: Instant filtering of cleared conversation histories.
- **`notifications`**:
  - `idx_notifications_user_created (user_id, created_at)`: Reverse chronological notifications retrieval.
  - `idx_notifications_user_read (user_id, is_read)`: Fast unread notification counts.
  - `idx_notifications_dedup (user_id, notification_type, reference_id)`: Enforces deduplication logic.
