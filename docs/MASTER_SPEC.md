# CHATSPHERE – COMPLETE FULL-STACK REAL-TIME MESSAGING APPLICATION
## MASTER DEVELOPMENT SPECIFICATION FOR ANTIGRAVITY AI AGENT

You are acting as a Senior Software Architect, Senior Java Developer, Spring Boot Engineer, React Developer, Database Engineer, Security Engineer, UI/UX Designer and QA Engineer.

Your responsibility is to build a complete, functional, maintainable and professional real-time messaging web application named **ChatSphere**.

This is a serious Java Full Stack Developer portfolio project. It must demonstrate professional software development practices and be suitable for GitHub, resume projects, college major projects and technical interviews.

IMPORTANT: Do not create incomplete code, dummy functionality, empty methods, placeholder APIs, fake data in production functionality or unfinished files.

Every implemented feature must work with the actual backend, database and frontend.

Do not simply generate a design or explain how to build it. Actually create and modify project files in the workspace, execute commands, compile the application, run tests and fix errors.

========================================================
SECTION 1: FINAL PROJECT REQUIREMENTS
========================================================

PROJECT NAME: ChatSphere

TAGLINE: Connect. Communicate. Collaborate.

PROJECT TYPE: Full-stack real-time messaging web application.

APPLICATION INSPIRATION: WhatsApp Web, Telegram and modern messaging platforms.

PRIMARY PURPOSE:
Build a professional messaging application where multiple registered users can communicate privately and in groups, exchange files and manage conversations.

FINAL DECISIONS:

Frontend:
- React.js
- JavaScript
- Vite
- HTML5
- CSS3
- Bootstrap 5

Backend:
- Java 21
- Spring Boot 3.x
- Spring MVC
- Spring Data JPA
- Hibernate
- Spring Security
- JWT

Database:
- MySQL 8

Real-time:
- Spring WebSocket
- STOMP
- SockJS fallback where appropriate

Caching:
- Redis
- Spring Cache

File storage:
- Cloudinary

Event processing:
- Apache Kafka in an advanced phase

Testing:
- JUnit 5
- Mockito
- Spring Boot Test
- React testing tools

Documentation:
- Swagger/OpenAPI
- README
- ER diagram
- Architecture documentation

Build:
- Maven
- npm

Architecture:
- Modular monolith initially.
- Future microservices migration must remain possible.

IMPORTANT RESTRICTIONS:
- No voice calling.
- No video calling.
- No unnecessary microservices initially.
- No paid third-party service dependency for basic messaging.
- End-to-end encryption must be implemented in an advanced phase using established cryptographic protocols.
- Normal messaging must work before introducing Redis, Kafka and encryption.

========================================================
SECTION 2: STRICT DEVELOPMENT RULES
========================================================

These rules are mandatory for every phase.

RULE 1: COMPLETE FILE GENERATION

Whenever you create a file, write its complete contents.

Never provide:
- Partial Java classes.
- Incomplete React components.
- Empty service methods.
- Empty repository methods.
- TODO implementations for required features.
- Placeholder return values.
- Fake authentication.
- Hardcoded chat data replacing database data.
- APIs that return success without performing the actual operation.

RULE 2: FILE CONSISTENCY

Before creating or modifying a file:
1. Inspect the existing project.
2. Read related files.
3. Understand existing classes and dependencies.
4. Check package names and imports.
5. Check existing API contracts.
6. Modify files without breaking existing features.

Do not create duplicate entities, DTOs, repositories, services or configurations.

RULE 3: BACKEND QUALITY

Use:
- Controller layer.
- Service layer.
- Repository layer.
- Entity layer.
- DTO layer.
- Mapper layer.
- Configuration layer.
- Exception handling layer.

Use constructor injection.

Keep controllers thin.

Keep business logic inside services.

Use DTOs for API requests and responses.

Never expose JPA entities directly through public APIs.

RULE 4: FRONTEND QUALITY

Use:
- Reusable React components.
- Centralized API services.
- Centralized authentication state.
- Custom hooks.
- Proper routing.
- Loading indicators.
- Error states.
- Form validation.
- Responsive layouts.

Do not put the entire application inside App.jsx.

RULE 5: TESTING

After each meaningful implementation:
- Run Maven compilation.
- Run backend tests.
- Run frontend build.
- Fix compilation errors.
- Fix runtime errors.
- Check API behavior.
- Verify database persistence.

RULE 6: NO FALSE COMPLETION

Never report a feature as completed until it has been implemented and tested.

Maintain a project progress document with:
- Completed features.
- Pending features.
- Known bugs.
- Test results.
- Next development steps.

RULE 7: VERSION COMPATIBILITY

Use compatible stable dependency versions.

Prefer:
- Java 21.
- Spring Boot 3.x.
- Compatible Spring Security.
- Compatible Spring Data JPA.
- Compatible WebSocket libraries.
- Compatible Redis client.
- Compatible MySQL driver.

Do not mix incompatible Spring versions.

Use Maven Wrapper and commit dependency configuration.

========================================================
SECTION 3: EXACT PROJECT DIRECTORY STRUCTURE
========================================================

Create:

ChatSphere/
│
├── chatsphere-backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/chatsphere/
│   │   │   │   ├── config/
│   │   │   │   ├── security/
│   │   │   │   ├── common/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── enums/
│   │   │   │   │   ├── constants/
│   │   │   │   │   └── util/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   └── dto/
│   │   │   │   ├── user/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── mapper/
│   │   │   │   ├── contact/
│   │   │   ├── conversation/
│   │   │   ├── message/
│   │   │   ├── group/
│   │   │   ├── attachment/
│   │   │   ├── notification/
│   │   │   ├── presence/
│   │   │   ├── websocket/
│   │   │   ├── cache/
│   │   │   ├── event/
│   │   │   ├── exception/
│   │   │   └── ChatSphereApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   │
│   ├── src/test/
│   ├── pom.xml
│   └── mvnw
│
├── chatsphere-frontend/
│   ├── public/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   │   ├── common/
│   │   │   ├── chat/
│   │   │   ├── sidebar/
│   │   │   ├── group/
│   │   │   ├── profile/
│   │   │   └── attachment/
│   │   ├── pages/
│   │   ├── layouts/
│   │   ├── services/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── websocket/
│   │   ├── utils/
│   │   ├── constants/
│   │   ├── styles/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
│
├── docker/
├── docs/
│   ├── architecture.md
│   ├── database-design.md
│   ├── api-documentation.md
│   ├── websocket-documentation.md
│   └── development-progress.md
│
├── .gitignore
├── .env.example
└── README.md

You may add additional files or subpackages when required, but do not remove the separation of responsibilities.

========================================================
SECTION 4: DATABASE DESIGN
========================================================

Use MySQL 8.

Use Flyway for all database schema creation and changes.

Do not depend on Hibernate ddl-auto=update in production.

Development configuration may use schema validation after Flyway migrations.

Create the following tables with appropriate relationships, indexes, unique constraints and foreign keys.

1. users

Fields:
- id
- public_id
- full_name
- username
- email
- password_hash
- profile_image_url
- bio
- account_status
- email_verified
- created_at
- updated_at
- last_seen_at

Rules:
- Username unique.
- Email unique.
- Public ID unique.
- Password must be BCrypt hashed.
- Never expose password_hash.

2. refresh_tokens

Fields:
- id
- user_id
- token_hash
- expires_at
- revoked
- created_at

3. user_contacts

Fields:
- id
- owner_id
- contact_user_id
- created_at

4. blocked_users

Fields:
- id
- blocker_id
- blocked_user_id
- created_at

5. user_privacy_settings

Fields:
- id
- user_id
- profile_photo_visibility
- last_seen_visibility
- online_status_visibility
- message_permission

6. conversations

Fields:
- id
- public_id
- conversation_type
- title
- image_url
- created_by
- created_at
- updated_at
- last_message_at

Conversation types:
- PRIVATE
- GROUP

7. conversation_members

Fields:
- id
- conversation_id
- user_id
- role
- joined_at
- left_at
- muted
- archived
- last_read_message_id
- unread_count

Constraints:
- Unique active membership per user and conversation.
- Appropriate indexes on user_id and conversation_id.

8. messages

Fields:
- id
- public_id
- client_message_id
- conversation_id
- sender_id
- message_type
- content
- reply_to_message_id
- forwarded_from_message_id
- edited
- deleted_for_everyone
- created_at
- updated_at
- deleted_at

Message types:
- TEXT
- IMAGE
- VIDEO
- DOCUMENT
- AUDIO
- SYSTEM

Add appropriate uniqueness for idempotency, such as sender_id plus client_message_id.

9. message_attachments

Fields:
- id
- message_id
- storage_public_id
- file_name
- file_type
- mime_type
- file_size
- storage_url
- thumbnail_url
- created_at

10. message_receipts

Fields:
- id
- message_id
- recipient_id
- delivered_at
- read_at

Unique constraint:
- message_id + recipient_id.

11. message_reactions

Fields:
- id
- message_id
- user_id
- emoji
- created_at

12. group_details

Fields:
- id
- conversation_id
- description
- created_by
- created_at

13. notifications

Fields:
- id
- user_id
- notification_type
- title
- message
- reference_id
- is_read
- created_at

14. email_verification_tokens

Fields:
- id
- user_id
- token_hash
- expires_at
- verified_at

Add additional tables only when necessary.

DATABASE DESIGN REQUIREMENTS:

- Use appropriate BIGINT internal IDs and UUID public IDs.
- Use UTC timestamps consistently.
- Use indexes on frequently searched and filtered columns.
- Avoid unnecessary eager loading.
- Prevent N+1 query problems.
- Use database transactions.
- Use proper cascade behavior.
- Avoid deleting conversation history accidentally.
- Keep database migrations versioned.
- Add indexes for conversation_id and created_at in messages.
- Add indexes for user_id and created_at in notifications.
- Add unique constraints where needed.
- Do not store actual file binaries in MySQL.
- Do not store private encryption keys on the server in the E2EE phase.

Create a complete ER diagram in docs/database-design.md.

========================================================
SECTION 5: BACKEND IMPLEMENTATION REQUIREMENTS
========================================================

A. USER AUTHENTICATION

Implement:
- Registration.
- Login.
- Logout.
- Refresh token.
- Email verification.
- Forgot password.
- Reset password.
- Change password.
- Secure logout.
- Account activation and deactivation.

Use:
- Spring Security.
- BCrypt.
- JWT access tokens.
- Refresh token rotation.
- Secure token storage.
- Request validation.
- Proper authorization.

Use secure HttpOnly cookies for refresh tokens with appropriate SameSite, Secure and CSRF configuration.

Do not log passwords or tokens.

B. USER MANAGEMENT

Implement:
- Get current user.
- Update profile.
- Upload profile image.
- Search users.
- Add contacts.
- Remove contacts.
- Block users.
- Unblock users.
- Privacy settings.
- Last seen.

C. CONVERSATIONS

Implement:
- Create private conversation.
- Find existing private conversation.
- List conversations.
- Get conversation details.
- Archive conversation.
- Unarchive conversation.
- Mute conversation.
- Delete conversation for current user where appropriate.

Prevent duplicate private conversations.

D. MESSAGING

Implement:
- Send text message.
- Retrieve messages.
- Reply to message.
- Edit own message.
- Delete message for self.
- Delete message for everyone.
- Forward message.
- React to message.
- Pin message.
- Search messages.
- Unread counts.
- Delivery receipts.
- Read receipts.

Validate sender identity and membership on the backend.

Persist messages before broadcasting them.

Use transactions for message persistence and associated records.

E. GROUP MANAGEMENT

Implement:
- Create group.
- Update group.
- Add members.
- Remove members.
- Leave group.
- Group admin.
- Promote/demote administrators.
- Group description.
- Group image.
- Group messages.
- Group permissions.
- Group member list.

F. ATTACHMENTS

Implement:
- Upload.
- Validation.
- Cloudinary storage.
- Preview.
- Download.
- Delete attachment where authorized.
- Multiple attachments.
- Upload progress.

G. NOTIFICATIONS

Implement:
- New message.
- Group invitation.
- Group member addition/removal.
- Mentions.
- Unread notifications.
- Mark as read.
- Notification history.

========================================================
SECTION 6: PAGINATION, SORTING AND FILTERING
========================================================

This section is mandatory.

Implement pagination, sorting and filtering consistently across all applicable listing APIs.

Use Spring Data Pageable and Page where offset pagination is appropriate.

Use cursor/keyset pagination for high-volume message history where appropriate.

A. CONVERSATION PAGINATION

Endpoint:

GET /api/conversations?page=0&size=20&sort=lastMessageAt,desc

Requirements:
- Default page = 0.
- Default size = 20.
- Maximum size = 50.
- Default sort = lastMessageAt descending.
- Return the latest conversations first.
- Include latest message preview.
- Include unread count.
- Include conversation type.
- Include participant information.
- Include pagination metadata.

Allowed sorting:
- lastMessageAt
- createdAt
- title

Never accept arbitrary database field names for sorting.

B. MESSAGE PAGINATION

Endpoint:

GET /api/conversations/{conversationId}/messages?size=30&before=cursor

Requirements:
- Return newest messages when no cursor is supplied.
- Load older messages when scrolling upward.
- Use stable cursor pagination.
- Sort by createdAt descending and id descending as a tie-breaker internally.
- Return messages in chronological display order to the frontend.
- Avoid loading all messages at once.
- Maximum page size = 100.
- Include hasMore and nextCursor metadata.

Do not use unbounded queries for chat history.

C. USER SEARCH

Endpoint:

GET /api/users/search?query=govind&page=0&size=20&sort=username,asc

Requirements:
- Search by username and full name.
- Case-insensitive search.
- Exclude blocked users where appropriate.
- Exclude the current user.
- Support pagination.
- Limit results.
- Escape wildcard characters where necessary.

D. MESSAGE SEARCH

Endpoint:

GET /api/messages/search?conversationId=...&query=hello&page=0&size=20&sort=createdAt,desc

Requirements:
- Search only conversations accessible to the current user.
- Search text messages only.
- Support date filtering.
- Support pagination.
- Use database indexes and efficient queries.
- Never expose private conversations through search.

E. NOTIFICATION PAGINATION

Endpoint:

GET /api/notifications?page=0&size=20&sort=createdAt,desc

Requirements:
- Latest first.
- Filter read/unread.
- Pagination metadata.
- Unread count.

F. COMMON PAGINATION RESPONSE

Use a consistent structure similar to:

{
  "success": true,
  "message": "Data retrieved successfully",
  "data": [],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}

For cursor pagination:

{
  "success": true,
  "data": [],
  "nextCursor": "opaque-cursor",
  "hasMore": true
}

Use the actual total count only where needed. Do not calculate expensive counts unnecessarily for cursor-based message history.

G. SORTING RULES

- Whitelist permitted sorting fields.
- Validate sort direction.
- Default to safe sorting.
- Prevent SQL injection.
- Use deterministic secondary ordering.
- Handle null values consistently.
- Never sort large message histories in memory.

H. FILTERING RULES

Support appropriate filters:
- Conversation type.
- Archived status.
- Muted status.
- Unread status.
- Message type.
- Date range.
- Notification read status.

Validate all filter parameters.

========================================================
SECTION 7: REDIS CACHING
========================================================

Redis caching is mandatory in the advanced integration phase.

Use Spring Cache and Redis.

Do not cache every database result blindly.

Implement caching for:

1. User public profile information.
2. Frequently accessed user information.
3. Conversation metadata.
4. Unread counts where consistency can be maintained.
5. Temporary online presence.
6. Temporary typing indicators.

Suggested cache names:

- userProfiles
- conversationMetadata
- unreadCounts
- userPresence
- typingIndicators

Suggested TTL:

- User profiles: 10 minutes.
- Conversation metadata: 2 minutes.
- Unread counts: 30 seconds.
- Presence: short TTL with heartbeat refresh.
- Typing indicators: 5 seconds.

These are initial values and should be configurable.

CACHE REQUIREMENTS:

- Implement cache-aside strategy.
- Read from Redis first.
- On cache miss, read from MySQL.
- Populate cache after successful database retrieval.
- Invalidate or update cache after writes.
- Handle Redis unavailability gracefully.
- MySQL must remain the source of truth.
- Never lose messages because Redis is unavailable.
- Do not cache private messages in shared cache keys.
- Include user/conversation identifiers in cache keys.
- Prevent stale data from leaking between users.
- Use appropriate serialization.
- Configure cache TTL centrally.

Implement appropriate cache invalidation when:
- User profile changes.
- Conversation changes.
- Group membership changes.
- Messages arrive.
- Read receipts update.
- Users block each other.

Do not rely on Redis alone for permanent message or receipt storage.

========================================================
SECTION 8: DATABASE PERFORMANCE AND OPTIMIZATION
========================================================

Implement:

- Indexing.
- Pagination.
- Sorting.
- Filtering.
- Efficient joins.
- DTO projections.
- Lazy loading where appropriate.
- Batch operations where useful.
- Transaction management.
- Connection pooling using HikariCP.
- Avoid N+1 queries.
- Prevent duplicate queries.
- Avoid unnecessary entity loading.

For message history:
- Index conversation_id, created_at and id.
- Use stable keyset pagination.
- Do not load full conversation history.
- Return lightweight DTOs.

For conversations:
- Fetch required participant details efficiently.
- Avoid one query per conversation.
- Optimize unread-count queries.

For user search:
- Use indexed searchable columns.
- Use normalized username/email search where appropriate.

Measure query performance during testing.

Do not add unnecessary database indexes without considering write overhead.

========================================================
SECTION 9: REAL-TIME WEBSOCKET AND STOMP
========================================================

Implement secure real-time messaging.

Endpoint:

/ws

Client destinations:

/app/chat.send
/app/chat.typing
/app/chat.read
/app/chat.delivered
/app/chat.reaction

Private destinations:

/user/queue/messages
/user/queue/notifications
/user/queue/receipts

Group destination:

/topic/conversations/{conversationId}

Important:

- Authenticate WebSocket connections.
- Authenticate STOMP CONNECT.
- Use JWT authentication.
- Authorize every subscription.
- Verify conversation membership.
- Prevent arbitrary subscription to private conversations.
- Validate message sender identity.
- Validate payloads.
- Persist messages before publishing.
- Handle reconnects.
- Handle disconnects.
- Handle duplicate events.
- Send messages to online recipients.
- Persist messages for offline recipients.

Message delivery:

1. Client creates a unique clientMessageId.
2. Client sends message.
3. Backend authenticates sender.
4. Backend validates conversation membership.
5. Backend checks idempotency.
6. Backend saves message.
7. Backend creates recipient receipt records.
8. Backend publishes the saved message.
9. Recipient acknowledges delivery.
10. Backend updates delivered status.
11. Recipient opens conversation.
12. Backend updates read status.

For groups, track receipts per recipient.

Use server-generated timestamps.

Do not trust the client to decide that a message has been delivered or read.

Typing indicators must be temporary and should not be permanently saved in MySQL.

========================================================
SECTION 10: FILE UPLOAD AND CLOUDINARY
========================================================

Use Cloudinary for external file storage.

Backend upload API:

POST /api/attachments/upload

Requirements:

- Multipart file upload.
- Validate file extension.
- Validate actual MIME type.
- Validate file signatures.
- Validate maximum file size.
- Generate unique identifiers.
- Restrict dangerous file types.
- Handle Cloudinary failures.
- Save metadata only after successful storage.
- Clean up uploaded files if database persistence fails.
- Prevent unauthorized access.
- Use signed URLs for private content when appropriate.

Supported formats:

IMAGE:
jpg, jpeg, png, webp.

VIDEO:
mp4, webm.

DOCUMENT:
pdf, doc, docx, txt, xls, xlsx, ppt, pptx.

AUDIO:
mp3, wav, m4a, ogg.

Set configurable size limits. Suggested initial limits:
- Images: 10 MB.
- Audio: 20 MB.
- Documents: 25 MB.
- Videos: 100 MB.

Never trust client-provided file metadata.

Frontend must show:
- Upload progress.
- Upload error.
- Preview.
- File name.
- File size.
- Download option.

Do not expose private Cloudinary credentials to React.

========================================================
SECTION 11: MESSAGE STATUS AND CHAT FEATURES
========================================================

Implement the following behavior precisely.

SENT:
The message has been successfully saved by the backend.

DELIVERED:
The recipient's client acknowledges receipt.

READ:
The recipient opens the relevant conversation and sends a read acknowledgement.

EDIT:
Only the sender can edit their own eligible messages.

DELETE FOR ME:
Hide the message for the requesting user without deleting it for other participants.

DELETE FOR EVERYONE:
Use appropriate authorization and a configurable time limit.

REPLY:
Store the referenced message ID and return a safe reply preview.

FORWARD:
Create a new message referencing the original source where permitted. Recheck destination permissions.

REACTION:
Allow a user to add or change their emoji reaction.

PIN:
Allow authorized users to pin important messages.

SEARCH:
Search only messages visible to the requesting user.

Implement group-specific receipt logic.

========================================================
SECTION 12: REACT FRONTEND REQUIREMENTS
========================================================

Build a professional WhatsApp-inspired responsive UI.

Design identity:

Application: ChatSphere.

Primary color: #075E54.
Secondary color: #128C7E.
Accent color: #25D366.
Light background: #F0F2F5.
Dark background: #111B21.
Incoming bubble: #FFFFFF.
Outgoing bubble: #DCF8C6.

DESKTOP LAYOUT:

LEFT SIDEBAR:
- User profile.
- Profile image.
- Search.
- New conversation.
- New group.
- Conversation list.
- Unread badges.
- Last message.
- Last message time.
- Archive.
- Settings.

CHAT AREA:
- Conversation header.
- Contact/group image.
- Contact/group name.
- Online status.
- Message history.
- Date separators.
- Incoming and outgoing messages.
- Timestamps.
- Sent/delivered/read indicators.
- Typing indicator.
- Reply preview.
- Image preview.
- Video player.
- Document attachment.
- Emoji reactions.
- Message input.

MOBILE LAYOUT:
- Responsive conversation list.
- Individual chat page.
- Back button.
- Mobile-friendly message input.
- Responsive attachment selection.
- No horizontal overflow.

AUTHENTICATION PAGES:
- Login.
- Registration.
- Forgot password.
- Reset password.
- Email verification.

PROFILE:
- Update name.
- Update bio.
- Update profile image.
- Change password.

SETTINGS:
- Privacy.
- Notifications.
- Theme.
- Account.
- Logout.

FRONTEND IMPLEMENTATION RULES:

- Use React Router.
- Use Axios.
- Create an API service layer.
- Create authentication context.
- Create WebSocket service.
- Create reusable message components.
- Use proper React keys.
- Avoid unnecessary re-renders.
- Clean up event listeners and socket subscriptions.
- Handle loading states.
- Handle API errors.
- Handle empty results.
- Handle expired sessions.
- Implement pagination and infinite scrolling.
- Preserve scroll position when loading older messages.
- Optimistically update UI only when rollback/error handling is implemented.
- Synchronize frontend state with server acknowledgements.

Do not use local hardcoded arrays as the real conversation database.

Demo data may only be used in clearly separated development seed scripts.

========================================================
SECTION 13: REST API CONTRACTS
========================================================

Use a consistent API response structure.

Success:

{
  "success": true,
  "message": "Operation successful",
  "data": {}
}

Failure:

{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email address"
    }
  ]
}

Use appropriate HTTP status codes.

AUTH:

POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh
POST /api/auth/verify-email
POST /api/auth/forgot-password
POST /api/auth/reset-password

USERS:

GET /api/users/me
PUT /api/users/me
GET /api/users/search
GET /api/users/{publicId}
POST /api/users/{publicId}/block
DELETE /api/users/{publicId}/block

CONTACTS:

GET /api/contacts
POST /api/contacts/{publicId}
DELETE /api/contacts/{publicId}

CONVERSATIONS:

GET /api/conversations
POST /api/conversations/private
GET /api/conversations/{publicId}
PUT /api/conversations/{publicId}/archive
PUT /api/conversations/{publicId}/mute

MESSAGES:

GET /api/conversations/{publicId}/messages
POST /api/messages
PUT /api/messages/{publicId}
DELETE /api/messages/{publicId}
POST /api/messages/{publicId}/forward
POST /api/messages/{publicId}/reactions
DELETE /api/messages/{publicId}/reactions
GET /api/messages/search

GROUPS:

POST /api/groups
GET /api/groups/{publicId}
PUT /api/groups/{publicId}
POST /api/groups/{publicId}/members
DELETE /api/groups/{publicId}/members/{userPublicId}
POST /api/groups/{publicId}/leave
PUT /api/groups/{publicId}/admins/{userPublicId}

ATTACHMENTS:

POST /api/attachments/upload
GET /api/attachments/{publicId}
DELETE /api/attachments/{publicId}

NOTIFICATIONS:

GET /api/notifications
PUT /api/notifications/{publicId}/read
PUT /api/notifications/read-all

PRESENCE:

GET /api/users/{publicId}/presence

Document all APIs using Swagger.

Ensure that the API implementation and React API calls use exactly matching endpoint names, HTTP methods and request formats.

========================================================
SECTION 14: ERROR HANDLING AND VALIDATION
========================================================

Create centralized exception handling using @RestControllerAdvice.

Handle:
- Resource not found.
- Duplicate email.
- Duplicate username.
- Invalid credentials.
- Unauthorized access.
- Forbidden access.
- Validation errors.
- Invalid JWT.
- Expired JWT.
- Database errors.
- File upload errors.
- WebSocket errors.
- Redis unavailable.
- External storage unavailable.

Use custom exception classes.

Never expose internal stack traces to clients in production.

Add:
- Request validation.
- Database constraint validation.
- API input validation.
- File validation.
- Authorization validation.
- Meaningful error messages.

Frontend must display user-friendly messages.

========================================================
SECTION 15: SECURITY REQUIREMENTS
========================================================

Implement:

- BCrypt.
- JWT authentication.
- Refresh token rotation.
- Secure logout.
- Authorization.
- CORS restrictions.
- CSRF protection where applicable.
- Secure cookies.
- Rate limiting.
- Input validation.
- File validation.
- WebSocket authorization.
- SQL injection prevention.
- XSS prevention.
- Safe logging.
- Secret management.
- HTTPS in production.

Do not:
- Hardcode passwords.
- Commit credentials.
- Expose JWT secrets.
- Store plain-text passwords.
- Trust client-provided user IDs.
- Allow unauthorized group access.
- Allow unauthorized private message access.
- Expose private files publicly.
- Store private E2EE keys on the server.

========================================================
SECTION 16: TESTING REQUIREMENTS
========================================================

Use JUnit 5, Mockito and Spring Boot Test.

Write tests for:

AUTH:
- Registration.
- Duplicate registration.
- Login.
- Invalid password.
- JWT validation.
- Refresh token rotation.
- Logout.

USERS:
- Profile update.
- Search.
- Block/unblock.
- Privacy settings.

CONVERSATIONS:
- Create private conversation.
- Prevent duplicate conversations.
- Conversation pagination.
- Unauthorized conversation access.

MESSAGES:
- Send message.
- Persist message.
- Duplicate prevention.
- Message pagination.
- Message sorting.
- Reply.
- Edit.
- Delete.
- Forward.
- Reactions.
- Read receipts.
- Delivery receipts.

GROUPS:
- Group creation.
- Add member.
- Remove member.
- Leave group.
- Admin authorization.
- Unauthorized group message.

ATTACHMENTS:
- Valid upload.
- Invalid file type.
- File size validation.
- Unauthorized download.
- Cloudinary failure.

REDIS:
- Cache hit.
- Cache miss.
- Cache invalidation.
- Redis unavailable fallback.

WEBSOCKET:
- Connection authentication.
- Private message delivery.
- Group message delivery.
- Unauthorized subscription.
- Typing event.
- Read receipt.
- Reconnection.

FRONTEND:
- Login.
- Registration.
- Protected routes.
- Conversation list.
- Message sending.
- Message history pagination.
- File upload.
- Error handling.

Test actual database behavior where appropriate using integration tests and isolated test configuration.

========================================================
SECTION 17: ADVANCED END-TO-END ENCRYPTION
========================================================

This is a later phase, not a prerequisite for basic messaging.

Use a maintained implementation of an established secure messaging protocol, such as the Signal Protocol, after evaluating current JavaScript client-library support and security maintenance.

Requirements:
- Client-side encryption.
- Client-side decryption.
- Key generation.
- Identity keys.
- Prekeys.
- Session establishment.
- Multiple device support.
- Encrypted attachment handling.
- Key rotation.
- Secure backup design.

The server must store ciphertext and required public-key/session metadata, not users' private encryption keys.

Do not invent cryptographic algorithms.

Do not claim E2EE merely because HTTPS or BCrypt is used.

Provide a security design document and test the key-exchange workflow before enabling E2EE in production.

========================================================
SECTION 18: REDIS AND KAFKA INTEGRATION
========================================================

Redis and Kafka should be added only after the core application works.

REDIS:
- Online/offline status.
- User presence.
- Temporary typing indicators.
- Cache user profiles.
- Cache conversation metadata.
- Cache selected unread counts.
- Cache invalidation.
- TTL management.

KAFKA:
- Message created event.
- Notification event.
- Group membership event.
- Background event processing.
- Retry handling.
- Dead-letter handling where appropriate.

Important reliability rules:

- MySQL remains the source of truth.
- Do not rely on Redis as permanent storage.
- Do not lose messages when Kafka is unavailable.
- Use transactional outbox or another reliable event-publication pattern where required.
- Design idempotent consumers.
- Avoid duplicate notifications.
- Handle retries safely.

========================================================
SECTION 19: DEPLOYMENT AND DEVOPS
========================================================

Prepare:

- Dockerfile for backend.
- Dockerfile for frontend.
- Docker Compose.
- MySQL container.
- Redis container.
- Kafka container in the advanced phase.
- Environment variables.
- Production profiles.
- Health checks.
- Logging.
- Swagger documentation.
- Deployment instructions.

Create .env.example.

Never commit .env files containing real credentials.

Create separate configurations for:
- Development.
- Testing.
- Production.

The project must be runnable locally without Cloudinary, Redis or Kafka during the initial development phase, using appropriate local development configurations where required.

Do not expose internal service credentials to the browser.

========================================================
SECTION 20: PHASE-BY-PHASE DEVELOPMENT EXECUTION
========================================================

Implement the entire project sequentially in these phases.

PHASE 1: FOUNDATION

- Inspect installed tools.
- Create root folders.
- Generate Spring Boot project.
- Configure Maven.
- Configure MySQL.
- Configure Flyway.
- Create health-check API.
- Create React Vite application.
- Configure Bootstrap.
- Configure Axios.
- Configure CORS.
- Test backend build.
- Test frontend build.
- Test database connection.

PHASE 2: AUTHENTICATION

- User entity.
- User repository.
- DTOs.
- Mapper.
- Service.
- Controller.
- Validation.
- BCrypt.
- Spring Security.
- JWT.
- Refresh tokens.
- Login.
- Registration.
- Logout.
- React authentication pages.

PHASE 3: USER MANAGEMENT

- Profile.
- Search.
- Contacts.
- Blocking.
- Privacy settings.
- Profile image.
- User pagination and sorting.

PHASE 4: PRIVATE CONVERSATIONS

- Conversation entity.
- Membership entity.
- Conversation repository.
- Conversation service.
- Conversation APIs.
- Conversation pagination.
- Conversation sorting.
- Unread counts.
- Conversation list UI.

PHASE 5: REAL-TIME MESSAGING

- Message entity.
- Message repository.
- Message DTOs.
- Message service.
- WebSocket configuration.
- STOMP authentication.
- Private subscriptions.
- Send/receive messages.
- Message history.
- Cursor pagination.
- Typing indicators.
- Delivery receipts.
- Read receipts.
- React messaging interface.

PHASE 6: GROUP MESSAGING

- Group entities.
- Group APIs.
- Member management.
- Group roles.
- Group authorization.
- Group WebSocket messaging.
- Group message receipts.
- Group UI.

PHASE 7: ATTACHMENTS

- Cloudinary integration.
- Upload validation.
- Image sharing.
- Video sharing.
- Document sharing.
- Audio sharing.
- Preview.
- Download.
- Upload progress.

PHASE 8: MESSAGE MANAGEMENT

- Reply.
- Edit.
- Delete for me.
- Delete for everyone.
- Forward.
- Reactions.
- Pin.
- Search.
- Archive.
- Mute.

PHASE 9: CACHING AND PERFORMANCE

- Redis configuration.
- Cache manager.
- Cache-aside.
- Cache invalidation.
- Online presence.
- Typing TTL.
- Database indexing.
- Query optimization.
- Pagination optimization.
- N+1 query prevention.
- Performance tests.

PHASE 10: NOTIFICATIONS AND EVENTS

- In-app notifications.
- Kafka integration.
- Event publishing.
- Event consumers.
- Reliable event handling.
- Push notifications.

PHASE 11: END-TO-END ENCRYPTION

- Security architecture.
- Key management.
- Protocol integration.
- Client-side encryption.
- Client-side decryption.
- Encrypted attachments.
- Security testing.

PHASE 12: FINAL TESTING AND DEPLOYMENT

- Full backend tests.
- Full frontend tests.
- API testing.
- WebSocket testing.
- Security testing.
- Docker.
- Production configuration.
- Deployment.
- Swagger.
- README.
- GitHub cleanup.
- Final documentation.

========================================================
SECTION 21: DEFINITION OF DONE
========================================================

A phase is complete only when:

1. All required files exist.
2. Every file contains complete implementation.
3. All required imports are correct.
4. Dependencies are configured.
5. Backend compiles successfully.
6. Frontend builds successfully.
7. Database migrations work.
8. Required APIs are functional.
9. Validation works.
10. Exception handling works.
11. Relevant tests pass.
12. Existing functionality remains working.
13. Documentation is updated.
14. No critical TODO remains in that phase.
15. No fake data replaces actual backend functionality.

For the final application, verify the following real-world workflow:

TEST 1:
Register User A and User B.

TEST 2:
Log in from two separate browsers.

TEST 3:
User A searches for User B.

TEST 4:
User A starts a private conversation.

TEST 5:
User A sends a message.

TEST 6:
User B receives it instantly.

TEST 7:
User B replies.

TEST 8:
User A receives the reply instantly.

TEST 9:
Test sent, delivered and read receipts.

TEST 10:
Disconnect User B and send a message from User A.

TEST 11:
Reconnect User B and verify message history.

TEST 12:
Create a group with multiple users.

TEST 13:
Send a group message.

TEST 14:
Upload and download images and documents.

TEST 15:
Verify pagination, sorting, filtering and search.

TEST 16:
Verify Redis caching and cache invalidation.

TEST 17:
Verify unauthorized users cannot access private conversations.

TEST 18:
Verify frontend and backend work after restarting the applications.

========================================================
SECTION 22: REQUIRED DOCUMENTATION
========================================================

Maintain these files throughout development:

README.md:
- Project overview.
- Technology stack.
- Features.
- Installation.
- Configuration.
- Run instructions.
- Screenshots.
- API overview.

docs/architecture.md:
- Architecture diagram.
- Module descriptions.
- Request flow.
- WebSocket flow.
- Redis flow.
- Kafka flow.

docs/database-design.md:
- Database tables.
- Relationships.
- ER diagram.
- Indexes.

docs/api-documentation.md:
- REST endpoints.
- Request examples.
- Response examples.
- Status codes.

docs/websocket-documentation.md:
- Connection endpoint.
- STOMP destinations.
- Message payloads.
- Authentication.
- Subscription authorization.

docs/development-progress.md:
- Phase status.
- Completed files.
- Pending work.
- Known bugs.
- Test results.

========================================================
SECTION 23: FINAL EXECUTION INSTRUCTIONS
========================================================

NOW START BUILDING CHATSPHERE.

FIRST:

1. Inspect the workspace.
2. Inspect installed Java, Maven, Node.js, npm and MySQL.
3. Check whether any existing files need to be preserved.
4. Prepare the project.
5. Create the root directory structure.

SECOND:

Implement Phase 1 completely.

Create every required file with complete code.

Configure:
- Spring Boot.
- Maven.
- Java 21.
- MySQL.
- Flyway.
- React.
- Vite.
- Bootstrap.
- Axios.
- CORS.
- Health-check endpoint.

THIRD:

Execute:
- Maven compile.
- Backend tests.
- Frontend npm build.
- Database connectivity test.

Fix every error that can be fixed in the current environment.

FOURTH:

Show:
- Complete folder structure.
- Files created.
- Important configuration.
- Database connection result.
- Backend build result.
- Frontend build result.
- Commands to run the applications.

FIFTH:

Continue implementing the remaining phases sequentially, maintaining the same code-quality rules and testing requirements.

Do not skip a phase or mark an unimplemented feature as complete.

If an external credential is missing, use a secure environment-variable placeholder and continue unrelated work.

If a feature depends on a previous incomplete phase, finish and verify that prerequisite first.

The final deliverable must be a fully functional, tested, responsive real-time messaging application with clean Java code, proper React components, MySQL persistence, Redis caching, pagination, sorting, filtering, WebSocket communication and professional documentation.

**MOST IMPORTANT: WRITE COMPLETE WORKING CODE FOR EVERY FILE. NEVER LEAVE INCOMPLETE FILES, MISSING METHODS, DUMMY APIS OR UNCONNECTED FRONTEND FEATURES.**