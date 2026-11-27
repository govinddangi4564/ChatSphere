# ChatSphere Development Progress

## Phase Summary

| Phase | Description | Status |
| :--- | :--- | :--- |
| **Phase 1** | Foundation (Spring Boot 3.3.9, React 18 + Vite, MySQL 8 Docker, Health API, Flyway) | Completed |
| **Phase 2A**| Authentication Backend (Users, Refresh Tokens, Email Verification, BCrypt, JWT, Security) | Completed |
| **Phase 2B**| Authentication Frontend (React Auth Context, Login, Register, Password Reset Pages, Guards) | Completed |
| **Phase 3** | User Management & Profile (Contacts, Blocking, Privacy Settings) | Completed |
| **Phase 4** | Private Conversations (Membership, Pagination, Conversation List) | Completed |
| **Phase 5A**| Message Persistence & History over REST (Keyset Pagination, Receipts, Unread Counts) | Completed |
| **Phase 5B**| Real-Time Messaging Backend (WebSocket, STOMP, Typing Indicators, Real-Time Delivery/Read) | Completed |
| **Phase 5C**| Real-Time Messaging Frontend (STOMP Client, Chat UI, Unread Badges, Delivery Ticks) | Completed |
| **Phase 6** | Group Messaging (Group Management, Permissions, Roles) | Completed |
| **Phase 7** | Attachments (Cloudinary Storage, Media Previews) | Pending |
| **Phase 8** | Message Management (Reactions, Replies, Edits, Deletions) | Pending |
| **Phase 9** | Caching and Performance (Redis, Query Optimization) | Pending |
| **Phase 10** | Notifications and Events (Kafka, In-App Notifications) | Pending |
| **Phase 11** | End-to-End Encryption (Client-side Cryptography) | Pending |
| **Phase 12** | Final Testing and Deployment (Dockerization, Production Tuning) | Pending |

---

## Phase 2B Progress Details (Auth Frontend)

### Completed Features
- **Routing & Navigation**:
  - Configured `react-router-dom` with `BrowserRouter`.
  - Implemented `ProtectedRoute` (redirects unauthenticated users to `/login`) and `PublicRoute` (redirects authenticated users to `/`).
- **State Management & Service Layer**:
  - `api.js`: Axios instance with request and response interceptors. Attaches Bearer JWT access tokens and performs single silent token refresh upon `401 Unauthorized` responses before retrying requests.
  - `authService.js`: Service layer matching backend `AuthController.java` endpoints (`register`, `login`, `refresh`, `logout`, `verifyEmail`, `forgotPassword`, `resetPassword`).
  - `userService.js`: Service for profile retrieval (`/api/users/me` with fallback).
  - `AuthContext.jsx` & `useAuth.js`: Global authentication context handling initial silent token refresh on application mount to restore active user sessions from HttpOnly cookies.
- **User Interface & Forms**:
  - `AuthLayout.jsx`: Responsive layout adhering to ChatSphere palette (`#075E54`, `#128C7E`, `#25D366`, `#F0F2F5`).
  - `LoginPage.jsx`: Input validation, server error alerts, loading state, password recovery and register navigation.
  - `RegisterPage.jsx`: Real-time validation for full name, username regex, valid email, password length, and password confirmation with success banner.
  - `ForgotPasswordPage.jsx`: Email recovery form with validation and clear submission feedback.
  - `ResetPasswordPage.jsx`: URL token extraction, password match validation, and success redirection.
  - `VerifyEmailPage.jsx`: Automatic email token validation with status indicators and login redirection.
  - `HomePage.jsx`: Authenticated dashboard presenting logged-in user details (`fullName`, `username`, `email`, `publicId`, `emailVerified`, `createdAt`), sign out button, and system health link.
- **Frontend Testing**:
  - Vitest + React Testing Library + JSDOM configured.
  - Tests covering `LoginPage`, `RegisterPage`, and `ProtectedRoute`.

### Real Test Results
- **Frontend Test Suite (`npm test`)**:
  - `LoginPage.test.jsx` (4 tests passed):
    - Renders sign in form with inputs and submit button
    - Displays validation errors on empty submission
    - Calls login on valid form submission
    - Displays server error on login failure
  - `RegisterPage.test.jsx` (3 tests passed):
    - Renders registration form fields
    - Validates password length and matching passwords
    - Submits registration successfully and shows success message
  - `ProtectedRoute.test.jsx` (3 tests passed):
    - Displays loading spinner while authentication status is resolving
    - Redirects unauthenticated users to `/login`
    - Renders protected child component when user is authenticated
  - **Total Frontend Tests: 10 passed, 0 failed.**
- **Frontend Production Build (`npm run build`)**: PASSED in 1.33s (110 modules transformed, zero build warnings/errors).
- **Backend Test Suite (`mvnw test`)**: PASSED (13 tests passed, 0 failed).

### Known Bugs
- None.

### Manual Verification Instructions
1. Start backend:
   ```powershell
   cd chatsphere-backend
   .\mvnw.cmd spring-boot:run
   ```
2. Start frontend:
   ```powershell
   cd chatsphere-frontend
   npm run dev
   ```
3. Open browser at [http://localhost:5173](http://localhost:5173):
   - Unauthenticated visitors are automatically routed to `/login`.
   - Click "Create account" to navigate to `/register`.
   - Submit invalid data to test real-time validation feedback.
   - Register a user and notice the success banner. Check the backend terminal console for the verification token.
   - Click the verification link or navigate to `/verify-email?token=<token>` to verify email.
   - Log in with valid credentials: observe redirection to `/` (HomePage), showing the logged-in user's name and details.
   - Refresh the browser page: notice the session persists via silent cookie refresh without logging the user out.
   - Click "Sign Out" to terminate the session and clear credentials.


---

## Phase 3 Progress Details (User Management & Contacts)

### Completed Features
- **Database Schema**:
  - `V3__user_management_schema.sql`: Created `user_contacts`, `blocked_users`, and `user_privacy_settings` with proper primary keys, unique constraints, foreign keys (`ON DELETE CASCADE`), and performance indexes.
- **Backend Architecture**:
  - Domain enums: `PrivacyVisibility` (`EVERYONE`, `CONTACTS_ONLY`, `NOBODY`), `MessagePermission` (`EVERYONE`, `CONTACTS_ONLY`).
  - JPA Entities: `UserContact`, `BlockedUser`, `UserPrivacySetting`.
  - Repositories: `UserContactRepository`, `BlockedUserRepository`, `UserPrivacySettingRepository`, and paginated custom search queries in `UserRepository`.
  - Service Layer: Single `@Service` classes `UserService` and `ContactService`.
    - Safe search with sort whitelist (`username`, `fullName`), case-insensitive partial matching, SQL wildcard escaping (`%`, `_`, `\`), and bidirectional blocked user exclusion.
    - Privacy enforcement logic for profile photo and last seen timestamp visibility based on contact relationship.
    - Profile update handling (`fullName`, `bio`, `profileImageUrl` with explicit note on Phase 7 cloud storage file uploads).
    - Presence / heartbeat last seen tracking.
    - Contacts addition, duplicate prevention, self-addition rejection, and removal.
    - Blocking, unblocking, and blocked user listing.
  - Controllers & DTOs: `UserController` (`/api/users/*`) and `ContactController` (`/api/contacts/*`) with OpenAPI Swagger annotations and standardized `ApiResponse<T>` / `PageResponse<T>`.
- **Frontend Architecture**:
  - `services/userService.js`: Complete user API integration (profile, search, block, unblock, privacy settings).
  - `services/contactService.js`: Contacts API integration (list, add, remove).
  - `hooks/useDebounce.js`: Reusable hook for responsive debounced user search.
  - `pages/ProfilePage.jsx`: View and edit user profile (full name, bio, image URL preview, account metadata, Phase 7 upload note).
  - `pages/SettingsPage.jsx`: Tabs for Privacy settings (photo, last seen, online status, message permissions), Blocked users management (with unblock buttons), and Theme toggle (light/dark with localStorage persistence).
  - `pages/ContactsPage.jsx`: Contacts list tab and debounced Find People tab with pagination, add contact, and block actions.
  - Navigation & App integration: Updated `Navbar.jsx` with responsive links and user avatar/logout, and `App.jsx` with protected routes `/profile`, `/settings`, and `/contacts`.
- **Automated Tests**:
  - Backend: `UserServiceTest` (12 unit tests), `ContactServiceTest` (5 unit tests), `UserControllerIntegrationTest` (4 full integration tests against real MySQL). All 35 backend tests passed.
  - Frontend: `ProfilePage.test.jsx`, `ContactsPage.test.jsx`, `SettingsPage.test.jsx`, `LoginPage.test.jsx`, `RegisterPage.test.jsx`, `ProtectedRoute.test.jsx`. All 16 frontend tests passed.

### Real Test Execution Results
- **Backend (`mvnw test`)**:
  - Total Tests Run: 35
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: `BUILD SUCCESS`
- **Frontend (`npm test`)**:
  - Test Files: 6 passed (6)
  - Tests: 16 passed (16)
  - Result: `ALL PASSED`
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.21s with zero errors.

### Known Bugs
- None.

### Next Steps
- Phase 4: Private Conversations (conversation creation, idempotency, conversation listing, pagination, and membership).


---

## Phase 4 Progress Details (Private Conversations)

### Completed Features
- **Database Schema**:
  - `V4__conversations_schema.sql`: Created `conversations` and `conversation_members` tables.
  - Enforced mathematical duplicate prevention at the database level using `private_pair_hash VARCHAR(50) UNIQUE NULL` computed as `min(user1, user2) + '_' + max(user1, user2)`.
  - Added indexes on `public_id`, `private_pair_hash`, `last_message_at`, `created_at`, `(user_id, archived)`, `(user_id, muted)`.
- **Backend Architecture**:
  - Domain enums: `ConversationType` (`PRIVATE`, `GROUP`), `MemberRole` (`MEMBER`, `ADMIN`).
  - JPA Entities: `Conversation`, `ConversationMember`.
  - Repositories: `ConversationRepository` and `ConversationMemberRepository` featuring membership queries with dynamic filtering (`type`, `archived`, `muted`, `unread`) and batch participant retrieval (`findAllByConversationIdIn`) to completely avoid N+1 queries.
  - Service Layer: `ConversationService` implementing create-or-find idempotency, self-conversation checks, bidirectional block enforcement, sort whitelist validation (`lastMessageAt`, `createdAt`, `title`), pagination limit validation (max size 50), participant projection with privacy checks, and archive/mute actions.
  - Controller: `ConversationController` (`/api/conversations/*`) with complete Swagger OpenAPI documentation and standardized `ApiResponse<T>` / `PageResponse<T>`.
- **Frontend Architecture**:
  - `services/conversationService.js`: Complete conversation API integration (`getConversations`, `getConversation`, `createOrFindPrivateConversation`, `archiveConversation`, `muteConversation`).
  - `components/chat/ConversationList.jsx`: Left sidebar featuring local search filtering, filter tabs (All, Unread, Archived), conversation cards with avatar, partner name, last message time, unread badges, and status icons.
  - `components/chat/ChatArea.jsx`: Active chat area with partner details, mute and archive controls, responsive mobile back button, and empty state indicating Phase 5 real-time messaging arrival.
  - `components/chat/NewConversationModal.jsx`: Modal to search contacts and users to immediately create or find a conversation.
  - `pages/HomePage.jsx`: Responsive WhatsApp-style 2-column layout (sidebar + main chat pane) with seamless mobile viewport toggle and empty state.
- **Automated Tests**:
  - Backend: `ConversationServiceTest` (6 unit tests), `ConversationControllerIntegrationTest` (full lifecycle integration test verifying duplicate prevention, member-only access 404, pagination limit rejection, sort whitelist rejection, archive/mute filtering). All 43 backend tests passed.
  - Frontend: `ConversationList.test.jsx`, `HomePageChat.test.jsx`, `ProfilePage.test.jsx`, `ContactsPage.test.jsx`, `SettingsPage.test.jsx`, `LoginPage.test.jsx`, `RegisterPage.test.jsx`, `ProtectedRoute.test.jsx`. All 21 frontend tests passed.

### Real Test Execution Results
- **Backend (`mvnw test`)**:
  - Total Tests Run: 43
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: `BUILD SUCCESS`
- **Frontend (`npm test`)**:
  - Test Files: 8 passed (8)
  - Tests: 21 passed (21)
  - Result: `ALL PASSED`
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.13s with zero errors.

### Known Bugs
- None.

### Next Steps
- Phase 5A: Message Persistence & History over REST (Keyset Pagination, Receipts, Unread Counts).

---

## Phase 5A Progress Details (Message Persistence & Keyset History over REST)

### Completed Features
- **Database Schema**:
  - `V5__messaging_schema.sql`: Created `messages` and `message_receipts` tables.
  - Enforced idempotency with unique constraint `uk_messages_sender_client_msg UNIQUE (sender_id, client_message_id)`.
  - Added composite index `idx_messages_conv_created_id (conversation_id, created_at, id)` for stable keyset pagination.
  - Added unique constraint `uk_message_receipt_msg_recipient UNIQUE (message_id, recipient_id)`.
  - Added `last_message_preview VARCHAR(255) NULL` to `conversations` table.
- **Backend Architecture**:
  - Domain enum: `MessageType` (`TEXT`, `IMAGE`, `VIDEO`, `DOCUMENT`, `AUDIO`, `SYSTEM`).
  - JPA Entities: `Message`, `MessageReceipt`, updated `Conversation` with `lastMessagePreview`.
  - Repositories:
    - `MessageRepository`: Implemented `findBySenderIdAndClientMessageId` for idempotency and JPQL queries (`findNewestMessages`, `findMessagesBeforeCursor`) with `JOIN FETCH m.sender` and tie-breaker sorting `ORDER BY m.createdAt DESC, m.id DESC`.
    - `MessageReceiptRepository`: Tracking per-recipient delivery and read timestamps.
  - DTO Records: `SendMessageRequest`, `MessageResponse`, `CursorApiResponse<T>`.
  - Service Layer:
    - `MessageService`:
      - Validates authenticated sender and conversation membership.
      - Enforces bidirectional blocking check for private conversations.
      - Enforces idempotency: returning existing message on duplicate `clientMessageId` without re-creating rows or duplicate unread increments.
      - Atomically inserts message, creates receipt rows for all other conversation members, increments recipients' unread counts, and updates conversation `last_message_at` and `last_message_preview`.
      - Keyset cursor pagination with stable Base64-encoded URL-safe cursor (`createdAt` + `id`), returning messages in chronological order with `hasMore` and `nextCursor`.
    - `ConversationService`: Updated summary mapping to serve real `conv.getLastMessagePreview()` and unread counts.
  - Controller: `MessageController` with endpoints `POST /api/messages` and `GET /api/conversations/{publicId}/messages`.
- **Documentation**:
  - `docs/api-documentation.md` updated with Section 6 covering Message Sending and Keyset Cursor Pagination contracts.

### Real Test Execution Results
- **Backend (`mvnw test`)**:
  - Total Tests Run: 50
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: `BUILD SUCCESS` (including 6 unit tests in `MessageServiceTest` and full integration test in `MessageControllerIntegrationTest`).
- **Frontend (`npm test`)**:
  - Test Files: 8 passed (8)
  - Tests: 21 passed (21)
  - Result: `ALL PASSED`
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.38s with zero errors.

### Known Bugs
- None.

### Next Steps
- Phase 5B: Real-time messaging with WebSocket, STOMP endpoints (`/ws`, `/app/chat.send`, `/topic/conversations/{id}`), delivery and read receipt acknowledgements, and typing indicators.

---

## Phase 5B Progress Details (Real-Time Messaging Backend)

### Completed Features
- **WebSocket & STOMP Configuration**:
  - Registered STOMP endpoints at `/ws` with allowed origin patterns `*` and SockJS fallback.
  - Configured application prefix `/app`, simple broker `/topic` and `/queue`, and user destination prefix `/user`.
  - Added `/ws/**` permit in `SecurityConfig.java`.
- **STOMP Inbound Channel Security Interceptor**:
  - `WebSocketAuthChannelInterceptor`:
    - `CONNECT`: Authenticates STOMP frames via `Authorization: Bearer <token>` or `X-Authorization`, validates JWT with `JwtService`, and populates `SecurityContext` / `accessor.setUser(authentication)`. Rejects unauthenticated or invalid tokens.
    - `SUBSCRIBE`: Enforces membership authorization for `/topic/conversations/{publicId}` destinations. Non-members attempting to eavesdrop on private conversation broadcasts are rejected with `AccessDeniedException`.
- **Presence Tracking (In-Memory)**:
  - `PresenceService` with concurrent session counting:
    - User connect registers online state.
    - User disconnect unregisters online state and updates `last_seen_at = Instant.now()` in MySQL upon total disconnect.
  - `WebSocketEventListener` listening for `SessionConnectedEvent` and `SessionDisconnectEvent`.
- **Service Layer & Receipts Integration**:
  - Reused existing `MessageService` without code duplication.
  - Added `markMessageDelivered`: updates recipient `message_receipts.delivered_at` and produces `ReceiptNotification`.
  - Added `markConversationRead`: updates all unread message receipts for recipient, resets member `unread_count = 0`, and produces `ReceiptNotification`s.
  - Added `getMessageSenderUsername` for recipient notification routing.
- **WebSocket Controller (`ChatWebSocketController`)**:
  - `/app/chat.send`: Derives sender from authenticated `Principal`, persists message via `MessageService`, and broadcasts to `/topic/conversations/{publicId}`.
  - `/app/chat.typing`: Ephemeral indicator broadcasted to `/topic/conversations/{publicId}/typing` without database persistence.
  - `/app/chat.delivered`: Updates delivery receipt and sends real-time notification to original sender's `/user/queue/receipts`.
  - `/app/chat.read`: Updates read receipts, resets recipient unread count, and sends real-time notifications to sender's `/user/queue/receipts`.
- **Documentation**:
  - `docs/websocket-documentation.md` completely documented with handshake specifications, STOMP frame architectures, and real payload examples.

### Real Test Execution Results
- **Backend (`mvnw test`)**:
  - Total Tests Run: 54
  - Failures: 0
  - Errors: 0
  - Skipped: 0
  - Result: `BUILD SUCCESS` (including 4 comprehensive STOMP integration test scenarios in `ChatWebSocketIntegrationTest`).
- **Frontend (`npm test`)**:
  - Test Files: 8 passed (8)
  - Tests: 21 passed (21)
  - Result: `ALL PASSED`
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.38s with zero errors.

### Known Bugs
- None.

### Next Steps
- Phase 5C: Real-time messaging frontend (STOMP client connection, message display in ChatArea, typing indicator UI, real-time message bubbles, unread badges, and delivery ticks).


---

## Phase 5C Progress Details (Real-Time Messaging Frontend)

### Completed Features
- **WebSocket & STOMP Service Layer**:
  - `websocketService.js`: STOMP client using `@stomp/stompjs` and `sockjs-client` fallback. Automatically injects `Authorization: Bearer <token>` on STOMP `CONNECT`, supports automatic exponential backoff reconnection, thread-safe destination subscriptions, and robust cleanup.
  - `useWebSocket.js`: React hook integrating `websocketService` with `AuthContext` to connect when logged in and disconnect on sign out. Provides helper dispatchers: `sendChatMessage` (with REST fallback), `sendTyping`, `sendDelivered`, and `sendRead`.
  - `messageService.js`: REST service for keyset message history (`/api/conversations/{id}/messages?before={cursor}&size=30`) and fallback message creation (`/api/messages`).
- **Chat Window UI (`ChatArea.jsx`)**:
  - WhatsApp-inspired aesthetic (`#075E54`, `#128C7E`, `#25D366`, `#DCF8C6`, `#F0F2F5`, `#111B21` in dark mode).
  - Header: Partner name, avatar, online indicator, and live typing status ("typing...").
  - Date Separator: Sticky badges ("Today", "Yesterday", or formatted dates) grouping messages by chronological day.
  - Incoming & Outgoing Bubbles: Distinct styling, micro-timestamps, and status ticks (`bi-clock` sending, `bi-check` sent, `bi-check-all` gray delivered, `bi-check-all` cyan read).
  - Error Handling & Retry: Failed sends render an alert badge with a retry button to re-dispatch with the original `clientMessageId`.
  - Typing Indicator: WhatsApp-style 3-dot pulsing wave animation (`TypingIndicator.jsx`).
  - Input Bar: Clean rounded input with emoji/attachment placeholders, auto-focus, and debounced typing indicator broadcast.
- **State Management & Keyset Pagination (`useChatMessages.js`)**:
  - Upward Infinite Scrolling: Listens to scroll position; when reaching top (`scrollTop <= 60`), fetches older page with keyset cursor and prepends messages while preserving exact scroll position (`scrollTop += newHeight - prevHeight`).
  - Optimistic Send & Reconciliation: Appends outgoing message immediately with `status: 'SENDING'` and UUID `clientMessageId`. On STOMP broadcast, reconciles in-place by `clientMessageId` and sets `status: 'SENT'`.
  - Live Receipts: Automatically dispatches delivery receipt when message arrives and read receipt when active conversation is viewed. Reconciles recipient updates from `/user/queue/receipts`.
  - Ephemeral Typing Events: Subscribes to `/topic/conversations/{id}/typing` and clears after 3 seconds of inactivity.
- **Live Sidebar Updates (`HomePage.jsx`)**:
  - Reconciles active and inactive conversation updates in real-time.
  - When a message arrives for an inactive chat, increments its `unreadCount` badge and moves it to the top of the conversation list.
  - Selecting a conversation automatically clears its local `unreadCount` to 0.

### Real Test Execution Results
- **Frontend Test Suite (`npm test`)**:
  - `ChatArea.test.jsx` (5 tests passed):
    - Empty state placeholder rendering
    - Chat header and initial message history loading
    - Optimistic send, input clear, and server acknowledgment
    - Retry button display on network failure and retry dispatch
    - Upward infinite scroll cursor pagination trigger
  - `HomePageChat.test.jsx` (2 tests passed)
  - `ConversationList.test.jsx` (3 tests passed)
  - `LoginPage.test.jsx` (4 tests passed)
  - `RegisterPage.test.jsx` (3 tests passed)
  - `ContactsPage.test.jsx` (2 tests passed)
  - `SettingsPage.test.jsx` (2 tests passed)
  - `ProfilePage.test.jsx` (2 tests passed)
  - `ProtectedRoute.test.jsx` (3 tests passed)
  - **Total Frontend Tests: 9 files passed, 26 tests passed, 0 failed.**
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.69s (263 modules transformed, zero errors).
- **Backend Test Suite (`mvnw test`)**:
  - Total Tests Run: 54
  - Failures: 0, Errors: 0, Skipped: 0
  - Result: `BUILD SUCCESS`

### Known Bugs
- None.

### Next Steps
- Phase 6: Group Messaging (group creation, group member management, admin roles, group message broadcast).


---

## Phase 6 Progress Details (Group Messaging)

### Completed Features
- **Database Schema**:
  - `V6__group_details_schema.sql`: Created `group_details` table with foreign key to `conversations(id)` ON DELETE CASCADE, description, creator foreign key, and indexes.
- **Backend Architecture & Group Lifecycle**:
  - Reused existing `Conversation` (with `conversation_type = 'GROUP'`) and `ConversationMember` entities.
  - `GroupDetail` entity & `GroupDetailRepository`.
  - `GroupService`:
    - `createGroup`: Creates conversation, group details, assigns creator as `ADMIN`, adds initial members with role `MEMBER`, and dispatches initial system message.
    - `getGroupDetails`: Validates active membership (`leftAt IS NULL`), loads group metadata, creator summary, current user's role, and active members list.
    - `updateGroup`: Enforces admin-only permissions. Updates title, description, and image URL; generates system message on title change.
    - `addMembers`: Enforces admin-only permissions. Handles re-joining for previous members (`leftAt = null`) or new member inserts; generates system message for each added user.
    - `removeMember`: Enforces admin-only permissions. Disallows self-removal (requires leave group). Sets `leftAt = Instant.now()`; generates system message.
    - `leaveGroup`: Allows any active member to leave (`leftAt = Instant.now()`). Auto-promotes the oldest active member to `ADMIN` if the last admin leaves; generates system message.
    - `updateMemberRole`: Enforces admin-only permissions to promote to `ADMIN` or demote to `MEMBER`; generates system message.
  - `GroupController`: REST endpoints at `/api/groups` (`POST /`, `GET /{id}`, `PUT /{id}`, `POST /{id}/members`, `DELETE /{id}/members/{userPublicId}`, `POST /{id}/leave`, `PUT /{id}/admins/{userPublicId}`).
- **Security & WebSocket Authorization**:
  - `MessageService`:
    - Added `sendSystemMessage(Conversation conversation, String content)`: persists `MessageType.SYSTEM` message, updates last message preview, and broadcasts via `SimpMessagingTemplate` to `/topic/conversations/{id}`.
    - Active membership validation: removed or left members are strictly blocked from sending messages or retrieving history with `403 Forbidden` (`AccessDeniedException`).
    - Per-recipient receipts: sends messages and generates receipts for all active group members, incrementing their individual unread counts.
  - `WebSocketAuthChannelInterceptor`:
    - Authorizes `/topic/conversations/{id}` and `/typing` subscriptions. Ensures removed or left members cannot subscribe (rejected with `AccessDeniedException`).
- **Frontend Architecture & Components**:
  - `groupService.js`: Full API integration for group operations.
  - `NewGroupModal.jsx`: Modal dialog to create new groups with title, description, image URL, and contact picker.
  - `GroupInfoModal.jsx`: Group info drawer/dialog showing details, participant count, admin badges, inline member management (promote/demote admin, remove member) for admins, and "Leave Group" button.
  - `ChatArea.jsx`: Group chat header with group icon, title, member count subtitle, click-to-open info modal, and group info header button.
  - `MessageBubble.jsx`: Renders sender names in green bold text above incoming group bubbles, and renders system messages as centered WhatsApp-style notice badges.
  - `ConversationList.jsx`: "New Group" button in sidebar header, group avatar icon display, and active group preview text.
  - `HomePage.jsx`: Coordinates new group creation, group updates, and leave actions.

### Real Test Execution Results
- **Backend Test Suite (`mvnw test`)**:
  - Total Tests Run: 65
  - Failures: 0, Errors: 0, Skipped: 0
  - Result: `BUILD SUCCESS` (including 10 unit tests in `GroupServiceTest` and full integration tests in `GroupControllerIntegrationTest`).
- **Frontend Test Suite (`npm test`)**:
  - Test Files: 11 passed (11)
  - Total Tests Run: 32 passed (32), 0 failed.
  - `GroupInfoModal.test.jsx`: 3 passed.
  - `NewGroupModal.test.jsx`: 2 passed.
  - `ChatArea.test.jsx`: 6 passed (including group headers, system messages, and sender names).
  - All existing tests (`HomePageChat`, `ConversationList`, `LoginPage`, `RegisterPage`, `ContactsPage`, `SettingsPage`, `ProfilePage`, `ProtectedRoute`) passed.
- **Frontend Production Build (`npm run build`)**:
  - Result: `vite build` completed cleanly in 1.60s (266 modules transformed, 0 errors).

### Known Bugs
- None.

### Next Steps
- Phase 7: Attachments (Cloudinary storage integration, file uploads, previews, validation, and progress).
