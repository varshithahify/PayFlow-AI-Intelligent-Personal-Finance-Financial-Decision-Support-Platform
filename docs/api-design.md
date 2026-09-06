# PayFlow AI
## API Design

---

# 1. API Overview

PayFlow AI exposes REST APIs through the Spring Boot backend.

The API layer provides controlled access to:

- Authentication
- Users
- Payments
- Gateways
- Reconciliation
- Investigation cases
- AI investigation
- Resolution
- Approvals
- Analytics
- Audit information

Base URL:

    /api/v1

All APIs use JSON unless otherwise specified.

---

# 2. API Design Principles

The APIs follow these principles:

1. RESTful resource-oriented design
2. Versioned API paths
3. Consistent HTTP status codes
4. Input validation
5. Authentication and authorization
6. Idempotency for payment creation
7. Consistent error responses
8. Pagination for large collections
9. No business logic in controllers
10. DTOs instead of exposing database entities directly

---

# 3. Authentication APIs

## 3.1 Login

### Endpoint

    POST /api/v1/auth/login

### Request

```json
{
  "username": "finance_user",
  "password": "password"
}