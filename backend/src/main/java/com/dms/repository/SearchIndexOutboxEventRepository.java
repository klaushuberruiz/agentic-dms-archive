package com.dms.repository;

import com.dms.domain.SearchIndexOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchIndexOutboxEventRepository extends JpaRepository<SearchIndexOutboxEvent, UUID> {
    List<SearchIndexOutboxEvent> findByProcessedAtIsNull();
    
    List<SearchIndexOutboxEvent> findByDocumentIdAndProcessedAtIsNull(String documentId);
}
