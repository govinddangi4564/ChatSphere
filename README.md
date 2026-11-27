# ChatSphere – Real-Time Messaging Platform

> **Connect. Communicate. Collaborate.**

ChatSphere is an enterprise-grade, full-stack real-time messaging web platform inspired by WhatsApp Web and Telegram. It is built using Java 21, Spring Boot 3.3.9, MySQL 8, Redis 7, React 18, Vite, and STOMP over WebSocket.

---

## Key Features

- **Authentication & Security:**
  - BCrypt password hashing, JWT access tokens, HttpOnly rotating refresh tokens.
  - Email verification workflow and password reset.
  - IP-based sliding window rate limiting on authentication endpoints.
  - Fine-grained privacy controls (Last Seen, Online Status, Profile Photo, Direct Message permissions).
- **Real-Time Messaging:**
  - 1-on-1 private conversations and multi-user group chats.
  - Real-time messaging via STOMP over WebSocket with SockJS fallback.
  - Per-recipient live delivery ticks (single grey -> double grey -> double blue).
  - Live typing indicators and Redis-backed presence tracking with heartbeat TTLs.
  - Keyset cursor pagination for infinite upward message history scrolling without offset degradation.
  - Optimistic client-side message sending with reconciliation and retry on failure.
- **Advanced Message Management:**
  - In-chat replies with quoted snippet preview.
  - Message editing (with `(edited)` indicator and live broadcast).
  - Delete for Me vs. Delete for Everyone (enforcing 2-hour window).
  - Message forwarding with block re-validation.
  - Emoji reactions with live count pills.
  - Pinned messages bar and full-text search with accessible conversation isolation.
- **Media & Attachments:**
  - Pluggable storage (Cloudinary in production, local-disk in development).
  - Validation by file extension, MIME type, and magic bytes / signatures.
  - Live upload progress, image/video previews, audio playback, and document downloads.
- **In-App Notifications:**
  - Automatic alerts for offline messages, group invitations, member updates, and `@mentions`.
  - Deduplication suppressing redundant unread alerts.
  - Live STOMP user queue pushes (`/user/queue/notifications`) and dropdown badge in Navbar.
- **Performance & Reliability:**
  - Cache-aside for user profiles and conversation metadata.
  - Graceful degradation: MySQL is the primary source of truth; if Redis is down, the system continues running seamlessly without data loss.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.9, Spring Data JPA, Spring Security, Hibernate 6, Flyway, Maven |
| **Database** | MySQL 8.0 (UTF-8mb4 character set) |
| **Cache & Real-time** | Redis 7.0, Spring WebSocket, STOMP |
| **Frontend** | React 18, Vite, Bootstrap 5, Bootstrap Icons, Axios, StompJS |
| **Storage** | Cloudinary / Local filesystem abstraction |
| **DevOps** | Multi-stage Dockerfiles, Docker Compose, Nginx |

---

## Project Structure

```text
ChatSphere/
├── chatsphere-backend/         # Spring Boot modular monolith
│   ├── src/main/java/          # Domain packages: auth, user, conversation, group, message, etc.
│   ├── src/main/resources/     # Flyway migrations (V1 through V10), properties
│   └── Dockerfile              # Multi-stage JDK 21 Alpine image
├── chatsphere-frontend/        # React + Vite single page app
│   ├── src/components/         # Chat, notification, auth, and common components
│   ├── src/services/           # Axios REST services and WebSocket manager
│   ├── nginx.conf              # Production Nginx reverse proxy configuration
│   └── Dockerfile              # Node 20 build + Nginx Alpine runtime
├── docker/                     # Docker Compose deployment files
│   └── docker-compose.yml      # Orchestrates MySQL, Redis, Backend, and Frontend
├── docs/                       # Architecture, database ER diagram, API, and progress specs
└── README.md
```

---

## Getting Started

### Prerequisites
- **JDK 21**
- **Node.js 20+** & **npm**
- **Docker & Docker Compose**

### Running Locally with Docker Compose

To start the full stack (MySQL, Redis, Spring Boot backend, and Nginx frontend):

```bash
# 1. Navigate to docker directory
cd docker

# 2. Start all containers
docker compose up -d

# 3. Access applications:
# - Frontend: http://localhost
# - Backend API: http://localhost:8080/api
# - Swagger Docs: http://localhost:8080/swagger-ui.html
```

### Running in Development Mode

```bash
# 1. Start MySQL and Redis
docker compose -f docker/docker-compose.yml up -d mysql redis

# 2. Run Backend
cd chatsphere-backend
./mvnw spring-boot:run

# 3. Run Frontend
cd ../chatsphere-frontend
npm install
npm run dev
# Frontend runs on http://localhost:5173
```

---

## API Overview

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/auth/register` | `POST` | Register a new user |
| `/api/auth/login` | `POST` | Authenticate user, receive JWT & set refresh cookie |
| `/api/auth/refresh` | `POST` | Rotate refresh token and issue new JWT |
| `/api/auth/logout` | `POST` | Revoke session and clear cookies |
| `/api/users/me` | `GET` / `PUT` | Fetch or update current user profile |
| `/api/conversations` | `GET` / `POST` | List conversations or initiate private chat |
| `/api/groups` | `POST` | Create a new group chat |
| `/api/messages` | `GET` / `POST` | Paginated message history (keyset cursor) / send |
| `/api/messages/{id}` | `PUT` / `DELETE`| Edit or delete (for me / for everyone) message |
| `/api/messages/search` | `GET` | Full-text search filtered to accessible conversations |
| `/api/notifications` | `GET` | Paginated in-app notifications with read filter |
| `/api/notifications/read-all` | `PUT` | Mark all notifications as read |
| `/ws` | `WS` | STOMP WebSocket connection endpoint |
