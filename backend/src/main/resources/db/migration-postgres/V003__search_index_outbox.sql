-- ============================================================
-- Flyway migration: V003__search_index_outbox.sql
-- Creates requirement_chunks and search_index_outbox_events tables
-- Rollback: DROP TABLE search_index_outbox_events; DROP TABLE requirement_chunks;
-- ============================================================

-- Requirement Chunks (parsed from uploaded documents for hybrid search)
CREATE TABLE requirement_chunks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    document_id         UUID NOT NULL,
    chunk_id            VARCHAR(100) NOT NULL,
    requirement_id      VARCHAR(100),
    parent_section      VARCHAR(255),
    chunk_text          TEXT NOT NULL,
    token_count         INTEGER NOT NULL,
    chunk_order         INTEGER NOT NULL,
    module              VARCHAR(100),
    approval_status     VARCHAR(20) NOT NULL DEFAULT 'draft',
    tags                VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_req_chunks_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uq_req_chunks_doc_chunk UNIQUE (document_id, chunk_id)
);

CREATE INDEX idx_req_chunks_tenant_id ON requirement_chunks (tenant_id);
CREATE INDEX idx_req_chunks_document_id ON requirement_chunks (document_id);
CREATE INDEX idx_req_chunks_requirement_id ON requirement_chunks (tenant_id, requirement_id);
CREATE INDEX idx_req_chunks_approval ON requirement_chunks (tenant_id, approval_status);

-- Search Index Outbox (transactional outbox for Azure AI Search sync)
CREATE TABLE search_index_outbox_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    entity_type         VARCHAR(50) NOT NULL DEFAULT 'REQUIREMENT_CHUNK',
    entity_id           UUID NOT NULL,
    action              VARCHAR(255) NOT NULL,
    payload             TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMP WITH TIME ZONE,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    max_retries         INTEGER NOT NULL DEFAULT 5,
    next_retry_at       TIMESTAMP WITH TIME ZONE,
    dead_lettered       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_outbox_unprocessed ON search_index_outbox_events (created_at)
    WHERE processed_at IS NULL AND dead_lettered = FALSE;
CREATE INDEX idx_outbox_retry ON search_index_outbox_events (next_retry_at)
    WHERE processed_at IS NULL AND dead_lettered = FALSE AND retry_count > 0;
CREATE INDEX idx_outbox_dead_letter ON search_index_outbox_events (tenant_id)
    WHERE dead_lettered = TRUE;
