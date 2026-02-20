# Code Review Task List — Cloud DMS

## Steering Rule Violations

### CRITICAL — Exposed JPA Entity in API Response
- [x] `DocumentController.getVersions()` returns `List<DocumentVersion>` (JPA entity) directly — must return a DTO (e.g. `VersionHistoryResponse`)
  - File: `backend/src/main/java/com/dms/controller/DocumentController.java` line 96
  - Steering: coding-standards.md → "DON'T: Expose JPA entities in API responses"

### CRITICAL — Missing `@Valid` on `@RequestBody` Parameters
- [x] `DocumentController.updateMetadata()` — `@RequestBody MetadataUpdateRequest` lacks `@Valid`
- [x] `HybridSearchController.hybridSearchPost()` — `@RequestBody HybridSearchRequest` lacks `@Valid`
- [x] `McpToolController` — all 5 endpoints lack `@Valid` on `@RequestBody`
  - Steering: coding-standards.md → "DO: @Valid on controller request bodies"

### CRITICAL — Missing `@PreAuthorize` on Controller Endpoints
- [x] `DocumentController` — none of its 10 endpoints have `@PreAuthorize`
  - Steering: coding-standards.md → "DO: @PreAuthorize for method-level security"

### CRITICAL — MCP Endpoints Use `@PreAuthorize("permitAll")`
- [x] `McpToolController` — all 5 endpoints use `permitAll`, bypassing authentication entirely
  - Requirement 2D.7: "authenticate MCP requests using Azure AD tokens with same RBAC enforcement as REST API"
  - Steering: coding-standards.md → "@PreAuthorize for method-level security"

### HIGH — Missing `@Version` (Optimistic Locking) on All Mutable Entities
- [x] `Document` — missing `@Version` on `entityVersion` field (field itself is also missing)
- [x] `DocumentType` — missing `@Version` on `entityVersion` field (field itself is also missing)
- [x] `Group` — missing `@Version` on `entityVersion` field (field itself is also missing)
- [x] `LegalHold` — missing `@Version` on `entityVersion` field (field itself is also missing)
  - DB schema has `entity_version BIGINT NOT NULL DEFAULT 0` on all four tables
  - Design doc specifies: "Include @Version on entityVersion for optimistic locking on mutable entities"

### HIGH — Missing Entity Fields (Schema ↔ Entity Mismatch)
- [x] `Document` — missing: `contentHash`, `idempotencyKey`, `deleteReason`
- [x] `DocumentVersion` — missing: `contentHash`
- [x] `DocumentType` — missing: `displayName`, `active`
- [x] `Group` — missing: `displayName`, `modifiedAt`, `modifiedBy`
  - All these columns exist in V001 migration but are absent from JPA entities

### HIGH — `@Data` on JPA Entities (Should Use `@Getter`/`@Setter`)
- [x] `Document`, `DocumentType`, `Group`, `LegalHold`, `DocumentVersion`, `AuditLog`, `UserGroup` all use `@Data`
  - `@Data` generates `equals()`/`hashCode()` using all fields, which is problematic for JPA lazy-loaded proxies and can cause `LazyInitializationException` or infinite loops
  - Steering: coding-standards.md → "Lombok @Data and @Builder for DTOs only"
  - `RequirementChunk` and `SearchIndexOutboxEvent` correctly use `@Getter`/`@Setter`

### HIGH — Business Logic in Domain Entities
- [x] `Document.isDeleted()` — contains logic (`deletedAt != null`)
- [x] `Document.hasActiveLegalHold()` — contains stream filtering logic
- [x] `DocumentType.getDefaultRetentionDays()` — alias method that belongs in service
  - Steering: architecture.md → "domain/ JPA entities (@Entity) - NO business logic"

### HIGH — TypeScript `any` Type Usage (Strict Mode Violation)
- [x] `audit.service.ts` — 6 occurrences of `Observable<any>` and `const params: any`
- [x] `hybrid-search.service.ts` — 4 occurrences (`request: any`, `Observable<any>`)
- [x] `retention.service.ts` — 3 occurrences of `Observable<any>`
- [x] `legal-hold.service.ts` — 1 occurrence of `Observable<any>`
- [x] `group.service.ts` — 1 occurrence of `Observable<any>`
- [x] `document-type.service.ts` — 1 occurrence of `Observable<any>`
  - Steering: coding-standards.md → "DO: TypeScript strict mode (no any)"
  - Fix: create proper TypeScript interfaces in `models/` for all response types

### MEDIUM — Inner Static DTOs in Controllers
- [x] `McpToolController` — 5 inner static request classes (should be in `dto/request/`)
- [x] `HybridSearchController` — 1 inner static `HybridSearchRequest` class
  - Steering: architecture.md → "dto/ Data Transfer Objects - API boundaries"

### MEDIUM — `DocumentVersion` Entity Missing `contentType` Field
- [x] `VersioningService.uploadNewVersion()` calls `previous.getContentType()` but `DocumentVersion` has no `contentType` field — this will fail at runtime
  - File: `backend/src/main/java/com/dms/service/VersioningService.java`

### LOW — Hardcoded Fallback Colors in `styles.scss`
- [x] `styles.scss` uses `#fafafa` and `#1c1b1f` as CSS variable fallbacks
  - Acceptable as fallbacks but noted for awareness
  - Steering: design-system.md → "ALWAYS use design tokens (never hardcode colors/spacing)"

## Open Functional Requirements (from tasks.md)

Tasks 0 and 1 are complete. Everything below is unimplemented.

### Task 2 — Domain Entities, DTOs, and Mappers
- [x] 2.1 Complete JPA entities — add missing fields listed above, add `@Version` optimistic locking
- [x] 2.2 Complete request/response DTOs — add validation annotations (`@NotNull`, `@Min`, `@Max`)
- [x] 2.3 Complete mapper classes — `DocumentMapper`, `AuditLogMapper`, `GroupMapper` are stubs
- [x] 2.4 Unit tests for mappers

### Task 3 — Exception Handling
- [x] 3.1 Create exception hierarchy — `DmsException` base exists but missing: `RetentionNotExpiredException`, `RateLimitExceededException`, `BlobStorageException`, `TenantMismatchException`, `SearchIndexUnavailableException`, `EmbeddingGenerationException`, `ConcurrentModificationException` (HTTP 409)
- [x] 3.2 Complete `GlobalExceptionHandler` — does not reveal document existence on 403 (done), but missing handling for new exception types

### Task 4 — Security and Multi-Tenancy
- [ ] 4.1 SecurityConfig — exists but needs JWT claim extraction review
- [x] 4.2 TenantContext — exists as stub, needs real JWT claim extraction
- [x] 4.3 AuthorizationService — exists as stub, needs group membership verification
- [x] 4.4 Rate limiting filter — `RateLimitingFilter` referenced in `SecurityConfig` but implementation not verified
- [ ] 4.5–4.8 Unit and property tests for auth, tenant isolation, RBAC, rate limiting (unit tests now added for auth, tenant context, and rate limiting; property tests still open)

### Task 6 — Repositories and Data Access
- [x] 6.1 DocumentRepository — partially implemented, missing JSONB query methods, full-text search
- [x] 6.2 Remaining repositories — `GroupRepository`, `UserGroupRepository`, `LegalHoldRepository`, `DocumentTypeRepository`, `AuditLogRepository` are stubs
- [x] 6.3 Integration tests for DocumentRepository

### Task 7 — Azure Blob Storage
- [ ] 7.1 BlobStorageConfig — exists as stub
- [ ] 7.2 BlobStorageService — exists but missing: SHA-256 content hash, PDF magic bytes validation, 100MB size enforcement, circuit breaker, SAS token generation
- [ ] 7.3–7.4 Unit and property tests

### Task 8 — Audit Logging
- [ ] 8.1 AuditService — partially implemented, missing: client IP capture, correlation ID propagation, authentication event logging
- [ ] 8.2–8.3 Unit and property tests

### Task 9 — Metadata Validation
- [ ] 9.1 MetadataValidationService — exists but needs JSON Schema draft-07 validation, field-level errors, schema evolution support
- [ ] 9.2–9.3 Property tests

### Task 11 — Document Upload
- [ ] 11.1 DocumentService.uploadDocument — missing: authorization check, active document type validation, PDF magic bytes check, content hash, idempotency key support, orphaned blob cleanup
- [x] 11.2 DocumentController upload — missing `@Valid`, `@PreAuthorize`
- [ ] 11.3–11.7 Unit, property, and integration tests

### Task 12 — Document Search
- [ ] 12.1 SearchService — partially implemented, missing: full-text tsvector search, proper pagination metadata
- [x] 12.2 SearchController — empty stub
- [ ] 12.3–12.7 Unit and property tests

### Task 13 — Document Download/Preview
- [ ] 13.1 Download/preview — partially implemented, missing: SAS tokens, Range request support, audit before streaming
- [ ] 13.2–13.3 Endpoints and tests

### Task 14 — Metadata Update
- [ ] 14.1 updateMetadata — exists but missing authorization check before update
- [x] 14.2 Controller endpoint — missing `@Valid`
- [ ] 14.3 Unit tests

### Task 15 — Document Versioning
- [x] 15.1 VersioningService — partially implemented, missing: content hash, audit logging, `contentType` field on entity
- [ ] 15.2 Controller endpoints — `getVersions` exposes JPA entity, missing restore endpoint
- [ ] 15.3–15.6 Unit and property tests

### Task 17 — Soft Delete and Restore
- [ ] 17.1 Soft delete — partially implemented, missing: `deleteReason` field on entity
- [x] 17.2 Controller endpoints — missing `@PreAuthorize`
- [ ] 17.3–17.4 Unit and property tests

### Task 18 — Hard Delete and Retention
- [ ] 18.1 Hard delete — missing: retention rule verification, legal hold check, all-version blob deletion, audit snapshot, admin restriction, bulk async support
- [ ] 18.2 RetentionService — partially implemented, missing: min retention enforcement, tenant-specific overrides, warning window notifications
- [ ] 18.3–18.6 Endpoints and tests

### Task 19 — Legal Hold Management
- [x] 19.1 LegalHoldService — stub only (`return false`), needs full implementation
- [x] 19.2 REST endpoints — not implemented
- [ ] 19.3–19.4 Unit and property tests (unit tests added; property tests still open)

### Task 20 — Group and Document Type Administration
- [x] 20.1 GroupService — empty stub
- [x] 20.2 DocumentTypeService — not found (may not exist)
- [x] 20.3 GroupController, DocumentTypeController — empty stubs
- [x] 20.4 Unit tests

### Task 21 — Bulk Download
- [ ] 21.1–21.3 Not implemented

### Task 22 — Audit Log Querying and Export
- [x] 22.1 AuditController — empty stub
- [ ] 22.2 Unit tests

### Task 24 — Document Parsing and Chunking
- [ ] 24.1 ChunkingService — exists as stub, needs Tika/POI integration
- [ ] 24.2–24.3 Unit and property tests

### Task 25 — Hybrid Search Indexing Pipeline
- [ ] 25.1 AzureSearchConfig/Client — stubs
- [ ] 25.2 EmbeddingService — stub
- [ ] 25.3 IndexingService — stub
- [ ] 25.4 SearchIndexOutboxProcessor — stub
- [ ] 25.5 IndexRebuildService — stub
- [ ] 25.6–25.8 Unit and property tests

### Task 26 — Hybrid Search Query
- [ ] 26.1 HybridSearchRouter — stub
- [ ] 26.2 SearchSecurityTrimmer — stub
- [ ] 26.3 SearchScoreMerger — stub
- [ ] 26.4 SearchFallbackHandler — stub
- [ ] 26.5 Hybrid search endpoint — exists but delegates to stub service
- [ ] 26.6–26.10 Unit and property tests

### Task 28 — MCP Server and IDE Integration
- [ ] 28.1 McpConfig/McpAuthorizationFilter — stubs, `permitAll` security
- [ ] 28.2 MCP tool handlers — partially wired but no real implementation
- [ ] 28.3 RetrievalAuditService — not found
- [ ] 28.4 ContextInjectionService — not found
- [ ] 28.5 validate_requirement_references — not implemented
- [ ] 28.6 Unit tests

### Task 29–30 — Monitoring and Operations
- [ ] 29.1–29.3 Index drift reconciliation, caching, monitoring metrics — not implemented
- [ ] 30.1 Health/metrics endpoints — actuator dependency present but custom checks not implemented

### Tasks 32–39 — Frontend Features
- [x] 32.1–32.5 Core services and models — partially scaffolded, `any` types throughout
- [ ] 33.1–33.3 Shared components — scaffolded as empty shells
- [ ] 34.1–34.5 Document management features — scaffolded, minimal implementation
- [ ] 35.1–35.2 Search features — scaffolded
- [ ] 36.1–36.4 Admin features — scaffolded
- [ ] 37.1 Legal hold features — scaffolded
- [ ] 38.1–38.3 Governance dashboards — scaffolded
- [ ] 39.1 Routing and lazy loading — exists

### Task 41 — Final Integration
- [ ] 41.1–41.3 Frontend-backend wiring, application properties, integration tests

## Build Issues
- [x] `pom.xml` — `azure-search-documents` has no version and is not in the Azure Spring BOM; add explicit version (e.g. `11.6.2`)
- [x] `pom.xml` — `flyway-database-postgresql` dependency was removed but may be needed for PG-specific Flyway features; verify (kept removed because artifact is unavailable for the Spring Boot-managed Flyway line in this project)
- [x] `DocumentVersion` entity missing `contentType` field causes compile error in `VersioningService`


