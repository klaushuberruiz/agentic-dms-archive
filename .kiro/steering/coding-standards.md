---
inclusion: always
---
# Coding Standards - AgenticDriverAcademy

## Java/Spring Boot

### DO
- Constructor injection: `@RequiredArgsConstructor` with `private final` fields
- `@Valid` on controller request bodies
- `@PreAuthorize` for method-level security
- `@Transactional(readOnly = true)` for read-only service methods
- Lombok `@Data` and `@Builder` for DTOs only
- Return `ResponseEntity<T>` from controllers

### DON'T
- `@Autowired` on fields
- Business logic in controllers or entities
- `RestTemplate` (use `WebClient`)
- Native SQL queries unless absolutely necessary
- Expose JPA entities in API responses

### Naming
- Classes: `PascalCase` (e.g., `DrivingLessonService`)
- Methods: `camelCase` (e.g., `findByStudentId`)
- Constants: `UPPER_SNAKE_CASE`
- Test methods: `shouldDoSomething_whenCondition`

## Angular/TypeScript

### DO
- TypeScript strict mode (no `any`)
- Standalone components (no NgModules)
- `OnPush` change detection
- Signals for reactive state
- Reactive Forms (not template-driven)
- Design system CSS variables for styling

### DON'T
- Business logic in components (extract to services)
- Direct `HttpClient` calls in components
- Inline styles for colors/spacing/typography
- Import from other feature modules
- `localStorage` for sensitive data (use memory)

### Naming
- Files: `kebab-case` (e.g., `driving-lesson.service.ts`)
- Components: `PascalCase` + `Component` suffix
- Services: `PascalCase` + `Service` suffix
- Interfaces: `PascalCase` (no `I` prefix)

## Database

### Migrations
- Flyway format: `V{version}__{description}.sql`
- Example: `V042__add_lesson_notes_column.sql`
- Always include rollback strategy in comments

### Conventions
- Tables: `snake_case` (e.g., `driving_lessons`)
- Columns: `snake_case` (e.g., `student_id`)
- Indexes: `idx_{table}_{column}`
- All tables have `tenant_id` column
