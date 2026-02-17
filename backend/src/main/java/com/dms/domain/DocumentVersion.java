package com.dms.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    @Column(name = "blob_path", nullable = false)
    private String blobPath;
    
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private DocumentVersion previousVersion;
}
