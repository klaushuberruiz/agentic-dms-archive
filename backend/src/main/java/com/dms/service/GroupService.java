package com.dms.service;

import com.dms.domain.Group;
import com.dms.domain.UserGroup;
import com.dms.exception.DocumentNotFoundException;
import com.dms.exception.ValidationException;
import com.dms.repository.GroupRepository;
import com.dms.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional
    public Group createGroup(String name, String displayName, String description, UUID parentGroupId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        groupRepository.findByNameAndTenantId(name, tenantId).ifPresent(existing -> {
            throw new ValidationException("Group with the same name already exists");
        });
        Group parentGroup = null;
        if (parentGroupId != null) {
            parentGroup = groupRepository.findByIdAndTenantId(parentGroupId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException("Parent group not found"));
        }
        Group group = Group.builder()
            .tenantId(tenantId)
            .name(name)
            .displayName(displayName == null || displayName.isBlank() ? name : displayName)
            .description(description)
            .parentGroup(parentGroup)
            .createdAt(Instant.now())
            .createdBy(userId)
            .modifiedAt(Instant.now())
            .modifiedBy(userId)
            .build();
        Group saved = groupRepository.save(group);
        auditService.logMetadataUpdate(saved.getId(), Map.of(), Map.of("action", "GROUP_CREATE", "name", saved.getName()));
        return saved;
    }

    @Transactional
    public Group updateGroup(UUID groupId, String displayName, String description, UUID parentGroupId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        Group group = groupRepository.findByIdAndTenantId(groupId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
        Group parentGroup = null;
        if (parentGroupId != null) {
            parentGroup = groupRepository.findByIdAndTenantId(parentGroupId, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException("Parent group not found"));
            ensureNoCycle(group, parentGroup);
        }
        group.setDisplayName(displayName == null || displayName.isBlank() ? group.getDisplayName() : displayName);
        group.setDescription(description == null ? group.getDescription() : description);
        group.setParentGroup(parentGroup);
        group.setModifiedAt(Instant.now());
        group.setModifiedBy(userId);
        Group saved = groupRepository.save(group);
        auditService.logMetadataUpdate(saved.getId(), Map.of(), Map.of("action", "GROUP_UPDATE"));
        return saved;
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Group group = groupRepository.findByIdAndTenantId(groupId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
        if (groupRepository.countByParentGroupId(groupId) > 0) {
            throw new ValidationException("Cannot delete group with child groups");
        }
        if (userGroupRepository.countByGroupId(groupId) > 0) {
            throw new ValidationException("Cannot delete group with members");
        }
        groupRepository.delete(group);
        auditService.logMetadataUpdate(groupId, Map.of(), Map.of("action", "GROUP_DELETE"));
    }

    @Transactional
    public void addUserToGroup(UUID groupId, String userId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String currentUserId = tenantContext.getCurrentUserId();
        Group group = groupRepository.findByIdAndTenantId(groupId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
        userGroupRepository.findByTenantIdAndUserIdAndGroupId(tenantId, userId, groupId).ifPresent(existing -> {
            throw new ValidationException("User is already a member of this group");
        });
        UserGroup userGroup = UserGroup.builder()
            .tenantId(tenantId)
            .userId(userId)
            .group(group)
            .assignedAt(Instant.now())
            .assignedBy(currentUserId)
            .build();
        userGroupRepository.save(userGroup);
        auditService.logMetadataUpdate(groupId, Map.of(), Map.of("action", "GROUP_ADD_MEMBER", "userId", userId));
    }

    @Transactional
    public void removeUserFromGroup(UUID groupId, String userId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        UserGroup userGroup = userGroupRepository.findByTenantIdAndUserIdAndGroupId(tenantId, userId, groupId)
            .orElseThrow(() -> new DocumentNotFoundException("Membership not found"));
        userGroupRepository.delete(userGroup);
        auditService.logMetadataUpdate(groupId, Map.of(), Map.of("action", "GROUP_REMOVE_MEMBER", "userId", userId));
    }

    @Transactional(readOnly = true)
    public Page<Group> listGroups(Pageable pageable) {
        return groupRepository.findByTenantId(tenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public Group getGroup(UUID groupId) {
        return groupRepository.findByIdAndTenantId(groupId, tenantContext.getCurrentTenantId())
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGroupHierarchy(UUID groupId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        Group start = groupRepository.findByIdAndTenantId(groupId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
        List<Group> children = groupRepository.findByParentGroupId(groupId);

        Set<UUID> visited = new HashSet<>();
        ArrayDeque<Group> parents = new ArrayDeque<>();
        Group current = start.getParentGroup();
        while (current != null && visited.add(current.getId())) {
            parents.push(current);
            current = current.getParentGroup();
        }
        return Map.of("group", start, "children", children, "parents", parents);
    }

    @Transactional(readOnly = true)
    public Page<UserGroup> getGroupMembers(UUID groupId, Pageable pageable) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        groupRepository.findByIdAndTenantId(groupId, tenantId)
            .orElseThrow(() -> new DocumentNotFoundException("Group not found"));
        List<UserGroup> members = userGroupRepository.findByGroupIdAndTenantId(groupId, tenantId);
        int start = Math.min((int) pageable.getOffset(), members.size());
        int end = Math.min(start + pageable.getPageSize(), members.size());
        List<UserGroup> content = members.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, members.size());
    }

    private void ensureNoCycle(Group group, Group newParent) {
        Group current = newParent;
        while (current != null) {
            if (current.getId().equals(group.getId())) {
                throw new ValidationException("Parent assignment creates a cycle");
            }
            current = current.getParentGroup();
        }
    }
}
