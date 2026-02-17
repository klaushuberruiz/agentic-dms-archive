package com.dms.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;
    
    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;
    
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;
    
    @Column(name = "blob_path", nullable = false)
    private String blobPath;
    
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;
    
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "modified_at")
    private Instant modifiedAt;
    
    @Column(name = "modified_by")
    private String modifiedBy;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Column(name = "deleted_by")
    private String deletedBy;
    
    @Column(name = "retention_expires_at", nullable = false)
    private Instant retentionExpiresAt;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentVersion> versions;
    
    @OneToMany(mappedBy = "document")
    private List<LegalHold> legalHolds;
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    public boolean hasActiveLegalHold() {
        return legalHolds != null && 
               legalHolds.stream().anyMatch(h -> h.getReleasedAt() == null);
    }
}
