# ChatSphere – Real-Time Messaging Platform

> **Connect. Communicate. Collaborate.**

ChatSphere is a modern, full-stack real-time messaging web application inspired by WhatsApp Web and Telegram. It is engineered with Spring Boot 3.x, MySQL 8, React (Vite), and WebSocket (STOMP).

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security, Flyway, Hibernate
- **Database:** MySQL 8
- **Frontend:** React (JavaScript), Vite, Bootstrap 5, Axios
- **Real-time:** Spring WebSocket + STOMP (Phase 5)
- **Caching:** Redis (Phase 9)
- **Object Storage:** Cloudinary (Phase 7)
- **Containerization:** Docker Compose

---

## Project Structure

```text
ChatSphere/
├── chatsphere-backend/      # Spring Boot application
├── chatsphere-frontend/     # React + Vite application
├── docker/                  # Docker Compose infrastructure (MySQL 8)
├── docs/                    # Architecture, design, API & progress specs
├── .env.example             # Template environment variables
└── README.md
```

---

## Getting Started

### Prerequisites
- JDK 21
- Node.js 20+ & npm
- Docker Desktop (for MySQL 8)

### Quick Start
1. **Clone and Configure Environment:**
   ```bash
   cp .env.example .env
   ```
2. **Start Database:**
   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```
3. **Run Backend:**
   ```bash
   cd chatsphere-backend
   ./mvnw spring-boot:run
   ```
4. **Run Frontend:**
   ```bash
   cd chatsphere-frontend
   npm install
   npm run dev
   ```

Frontend runs at `http://localhost:5173` and backend runs at `http://localhost:8080`.
API documentation is accessible at `http://localhost:8080/swagger-ui.html`.
