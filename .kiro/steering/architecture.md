---
inclusion: always
---
# Architecture Guidelines - AgenticDriverAcademy

## Project Overview
AgenticDriverAcademy is a driving school management system with:
- **Backend**: Spring Boot 3.x (Java 17, Maven)
- **Frontend**: Angular 17+ (TypeScript strict, standalone components)
- **Database**: PostgreSQL 15 (H2 for local dev)
- **Cloud**: Azure (App Service, Key Vault, Blob Storage)

## Layer Architecture

### Backend Structure
```
backend/src/main/java/com/driveracademy/backend/
├── controller/     # REST endpoints (@RestController) - NO business logic
├── service/        # Business logic (@Service) - transaction boundaries here
├── repository/     # Data access (@Repository) - JPA queries only
├── domain/         # JPA entities (@Entity) - NO business logic
├── dto/            # Data Transfer Objects - API boundaries
├── config/         # Spring configuration (@Configuration)
├── exception/      # Custom exceptions and @ControllerAdvice
└── mapper/         # Entity ↔ DTO mappers
```

### Frontend Structure
```
frontend/src/app/
├── core/           # Singleton services (AuthService, guards, interceptors)
├── shared/         # Reusable components, pipes, directives
├── features/       # Feature modules (lazy loaded)
│   ├── instructor/ # Instructor portal features
│   ├── student/    # Student portal features
│   ├── lessons/    # Lesson management
│   └── admin/      # Admin features
├── models/         # TypeScript interfaces
└── services/       # API services
```

## Dependency Rules
- Controllers → Services only (never repositories)
- Services → Repositories + other Services
- Repositories → NO business logic
- Features → NEVER import from other features
- Shared → NEVER import from features

## Multi-Tenancy
- Every entity has `tenant_id` field
- All queries MUST filter by tenant_id
- TenantContext extracts tenant from JWT
- Cross-tenant access = HTTP 403

## Key Patterns
- Constructor injection only (no @Autowired on fields)
- DTOs at API boundaries (never expose entities)
- @Transactional on service methods that modify data
- Flyway for all database migrations
- Standalone Angular components (no NgModules)
