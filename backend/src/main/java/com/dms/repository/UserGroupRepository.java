package com.dms.repository;

import com.dms.domain.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    List<UserGroup> findAllByTenantIdAndUserId(UUID tenantId, String userId);
}
