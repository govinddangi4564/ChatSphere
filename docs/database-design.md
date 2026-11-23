# ChatSphere Database Design

## 1. Strategy & Conventions
- **Database Engine:** MySQL 8 with InnoDB engine, `utf8mb4` encoding and `utf8mb4_unicode_ci` collation.
- **Migration Engine:** Flyway versioned migrations (`V1__...sql`, `V2__...sql`).
- **Identifiers:** Internal `id BIGINT AUTO_INCREMENT PRIMARY KEY` for relational integrity and foreign keys; `public_id VARCHAR(36) NOT NULL UNIQUE` (UUID) for API consumption.
- **Audit Columns:** `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP`.

## 2. Phase-by-Phase Table Roadmap
- **Phase 1 (Foundation):** Baseline migration `V1__init_baseline.sql` verifying Flyway setup with validation.
- **Phase 2 (Auth):** `users`, `refresh_tokens`, `email_verification_tokens`.
- **Phase 3 (User Management):** `user_contacts`, `blocked_users`, `user_privacy_settings`.
- **Phase 4 (Private Conversations):** `conversations`, `conversation_members`.
- **Phase 5 (Messaging):** `messages`, `message_receipts`.
- **Phase 6 (Groups):** `group_details`.
- **Phase 7 (Attachments):** `message_attachments`.
- **Phase 8 (Message Management):** `message_reactions`.
- **Phase 10 (Notifications):** `notifications`.
