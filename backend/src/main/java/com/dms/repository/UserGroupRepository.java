package com.dms.repository;

import com.dms.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    List<UserGroup> findAllByTenantIdAndUserId(UUID tenantId, String userId);
    
    Optional<UserGroup> findByUserIdAndGroupId(String userId, UUID groupId);
    
    List<UserGroup> findByGroupId(UUID groupId);

    List<UserGroup> findByGroupIdAndTenantId(UUID groupId, UUID tenantId);

    long countByGroupId(UUID groupId);

    Optional<UserGroup> findByTenantIdAndUserIdAndGroupId(UUID tenantId, String userId, UUID groupId);
}
