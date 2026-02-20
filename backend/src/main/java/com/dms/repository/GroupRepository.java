package com.dms.repository;

import com.dms.domain.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findAllByTenantId(UUID tenantId);
    
    Optional<Group> findByNameAndTenantId(String name, UUID tenantId);

    Optional<Group> findByIdAndTenantId(UUID id, UUID tenantId);
    
    Page<Group> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<Group> findByParentGroupId(UUID parentGroupId);

    long countByParentGroupId(UUID parentGroupId);
}

