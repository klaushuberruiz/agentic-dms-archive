-- ============================================================
-- Flyway migration: V001__initial_schema.sql (H2)
-- Creates all core tables for local development on H2
-- ============================================================

CREATE TABLE document_types (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    entity_version      BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    display_name        VARCHAR(255),
    description         TEXT,
    metadata_schema     JSONB NOT NULL,
    allowed_groups      UUID ARRAY NOT NULL,
    retention_days      INTEGER NOT NULL DEFAULT 2555,
    min_retention_days  INTEGER NOT NULL DEFAULT 0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by          VARCHAR(255) NOT NULL,
    modified_at         TIMESTAMP WITH TIME ZONE,
    modified_by         VARCHAR(255),
    CONSTRAINT uq_document_types_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_document_types_tenant_id ON document_types (tenant_id);
CREATE INDEX idx_document_types_active ON document_types (tenant_id, active);

CREATE TABLE groups (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    entity_version      BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    display_name        VARCHAR(255),
    description         TEXT,
    parent_group_id     UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by          VARCHAR(255) NOT NULL,
    modified_at         TIMESTAMP WITH TIME ZONE,
    modified_by         VARCHAR(255),
    CONSTRAINT fk_groups_parent FOREIGN KEY (parent_group_id) REFERENCES groups (id),
    CONSTRAINT uq_groups_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_groups_tenant_id ON groups (tenant_id);
CREATE INDEX idx_groups_parent ON groups (parent_group_id);

CREATE TABLE user_groups (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    group_id            UUID NOT NULL,
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    assigned_by         VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_groups_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT uq_user_groups_membership UNIQUE (tenant_id, user_id, group_id)
);

CREATE INDEX idx_user_groups_tenant_id ON user_groups (tenant_id);
CREATE INDEX idx_user_groups_user_id ON user_groups (tenant_id, user_id);
CREATE INDEX idx_user_groups_group_id ON user_groups (group_id);

CREATE TABLE documents (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    entity_version      BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    document_type_id    UUID NOT NULL,
    current_version     INTEGER NOT NULL DEFAULT 1,
    metadata            JSONB NOT NULL,
    metadata_tsv        TEXT,
    blob_path           VARCHAR(500) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    content_type        VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    content_hash        VARCHAR(128),
    idempotency_key     VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by          VARCHAR(255) NOT NULL,
    modified_at         TIMESTAMP WITH TIME ZONE,
    modified_by         VARCHAR(255),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    deleted_by          VARCHAR(255),
    delete_reason       TEXT,
    retention_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_documents_document_type FOREIGN KEY (document_type_id) REFERENCES document_types (id),
    CONSTRAINT uq_documents_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_documents_tenant_id ON documents (tenant_id);
CREATE INDEX idx_documents_document_type_id ON documents (tenant_id, document_type_id);
CREATE INDEX idx_documents_created_at ON documents (tenant_id, created_at);
CREATE INDEX idx_documents_not_deleted ON documents (tenant_id, deleted_at);
CREATE INDEX idx_documents_retention ON documents (retention_expires_at, deleted_at);
CREATE INDEX idx_documents_idempotency ON documents (tenant_id, idempotency_key);

CREATE TABLE document_versions (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    document_id         UUID NOT NULL,
    version_number      INTEGER NOT NULL,
    blob_path           VARCHAR(500) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    content_type        VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    content_hash        VARCHAR(128),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by          VARCHAR(255) NOT NULL,
    previous_version_id UUID,
    CONSTRAINT fk_doc_versions_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_versions_previous FOREIGN KEY (previous_version_id) REFERENCES document_versions (id),
    CONSTRAINT uq_doc_versions_number UNIQUE (document_id, version_number)
);

CREATE INDEX idx_doc_versions_document_id ON document_versions (document_id);
CREATE INDEX idx_doc_versions_tenant_id ON document_versions (tenant_id);

CREATE TABLE legal_holds (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    entity_version      BIGINT NOT NULL DEFAULT 0,
    tenant_id           UUID NOT NULL,
    document_id         UUID NOT NULL,
    case_reference      VARCHAR(255) NOT NULL,
    reason              TEXT NOT NULL,
    placed_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    placed_by           VARCHAR(255) NOT NULL,
    released_at         TIMESTAMP WITH TIME ZONE,
    released_by         VARCHAR(255),
    release_reason      TEXT,
    CONSTRAINT fk_legal_holds_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE INDEX idx_legal_holds_tenant_id ON legal_holds (tenant_id);
CREATE INDEX idx_legal_holds_document_id ON legal_holds (document_id);
CREATE INDEX idx_legal_holds_active ON legal_holds (document_id, released_at);
CREATE INDEX idx_legal_holds_case_ref ON legal_holds (tenant_id, case_reference);

CREATE TABLE audit_logs (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    correlation_id      UUID NOT NULL,
    action              VARCHAR(50) NOT NULL,
    entity_type         VARCHAR(50) NOT NULL,
    entity_id           UUID NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    client_ip           VARBINARY(19),
    timestamp           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    details             JSONB NOT NULL
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id, timestamp);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs (entity_id, timestamp);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id, timestamp);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, timestamp);
CREATE INDEX idx_audit_logs_correlation ON audit_logs (correlation_id);
