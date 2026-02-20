package com.dms.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalHold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "case_reference", nullable = false)
    private String caseReference;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;
    
    @Column(name = "placed_by", nullable = false)
    private String placedBy;
    
    @Column(name = "released_at")
    private Instant releasedAt;
    
    @Column(name = "released_by")
    private String releasedBy;
    
    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    @Version
    @Column(name = "entity_version", nullable = false)
    private Long entityVersion;
}
