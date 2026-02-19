package com.dms.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Type(JsonType.class)
    @Column(name = "metadata_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadataSchema;
    
    @Column(name = "allowed_groups", columnDefinition = "uuid[]", nullable = false)
    private UUID[] allowedGroups;
    
    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;
    
    @Column(name = "min_retention_days", nullable = false)
    private Integer minRetentionDays;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "modified_at")
    private Instant modifiedAt;
    
    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
