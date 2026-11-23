# ChatSphere Architecture

## 1. System Overview

ChatSphere is structured initially as a clean, highly modular monolith designed for future microservices decomposition. 

```
+-----------------------------------------------------------+
|                   React Frontend (Vite)                   |
|       Bootstrap 5 | Axios HTTP | STOMP WebSocket Client    |
+-----------------------------------------------------------+
                              |
                     REST / HTTP & WebSocket
                              |
+-----------------------------------------------------------+
|               Spring Boot 3.x Application                 |
|                                                           |
|  [Security / JWT]   [OpenAPI / CORS]    [WebSocket Broker] |
|                                                           |
|  Controllers -> Services (Single @Service) -> Repositories |
+-----------------------------------------------------------+
                              |
               +--------------+--------------+
               |                             |
      +-----------------+           +-----------------+
      |  MySQL 8 (JPA)  |           |  Redis (Cache)  |
      | Flyway Migration|           |  (Phase 9)      |
      +-----------------+           +-----------------+
```

## 2. Core Architectural Principles
1. **Layered Separation:** Thin Controllers -> Business logic in single `@Service` classes -> Spring Data JPA repositories.
2. **DTO & Entity Isolation:** JPA entities are never exposed in REST controllers. All API contracts use Java record DTOs.
3. **Public ID Addressing:** Internal autoincrement `BIGINT` IDs are strictly kept inside the service and persistence layers; all API endpoints and external references expose UUID-based `publicId`.
4. **Standard Response Model:** Every endpoint responds with the standardized `ApiResponse<T>` or `ErrorResponse` envelope handled through a central `@RestControllerAdvice`.
5. **Database Evolution:** Schema changes are strictly version-controlled through Flyway migrations with `ddl-auto=validate`.
