# Cloud-Native Document Management System (DMS)

Enterprise-grade document management system with hybrid requirements integration for AI-driven IDEs.

## Stack

- **Cloud**: Microsoft Azure (App Service, Blob Storage, PostgreSQL, AI Search, Key Vault)
- **Backend**: Spring Boot 3.x (Java 17, Maven)
- **Frontend**: Angular 17+ (TypeScript strict, standalone components)
- **Database**: Azure Database for PostgreSQL 15
- **Storage**: Azure Blob Storage
- **Search**: Hybrid (Lucene keyword + Vector RAG via Azure AI Search)
- **Integration**: MCP Tool Server for AI agents

## Features

- PDF document management (invoices, forms, letters, price lists)
- JSONB metadata with dynamic schema validation
- Document versioning with immutable history
- Role-based access control (RBAC) with multi-tenancy
- Retention policies and legal holds
- Hybrid search (keyword + semantic vector)
- Requirement chunking and embedding generation
- MCP integration for AI IDE agents
- Comprehensive audit logging
- Traceability dashboard

## Project Structure

```
.
├── backend/                 # Spring Boot 3.x REST API
├── frontend/                # Angular 17+ SPA
├── mcp-server/              # Model Context Protocol server
├── .kiro/                   # Kiro IDE configuration
│   ├── specs/               # Specifications
│   └── steering/            # Architecture guidelines
└── docs/                    # Additional documentation
```

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.8+
- Azure CLI
- PostgreSQL 15 (local dev: H2)

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

### MCP Server

```bash
cd mcp-server
mvn spring-boot:run
```

## Documentation

- [Requirements](.kiro/specs/cloud-document-management-system/requirements.md)
- [Design](.kiro/specs/cloud-document-management-system/design.md)
- [Architecture](steering/architecture.md)
- [Coding Standards](steering/coding-standards.md)
- [Testing Guidelines](steering/testing.md)

## License

Proprietary
