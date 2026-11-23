# ChatSphere API Documentation

## 1. Standard Response Formats

### Success Response (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

### Error Response (`ErrorResponse`)
```json
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
```

## 2. Endpoints: Phase 1 Foundation

### Health Check
- **Endpoint:** `GET /api/health`
- **Access:** Public (permitted in Spring Security)
- **Response Example:**
```json
{
  "success": true,
  "message": "ChatSphere API is operational",
  "data": {
    "status": "UP",
    "timestamp": "2026-11-23T18:50:00Z",
    "database": "UP",
    "version": "1.0.0"
  }
}
```

### Swagger Documentation
- **UI:** `GET /swagger-ui/index.html`
- **OpenAPI JSON:** `GET /v3/api-docs`
