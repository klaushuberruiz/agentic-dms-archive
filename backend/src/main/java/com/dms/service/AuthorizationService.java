package com.dms.service;

import com.dms.domain.Document;
import com.dms.domain.Group;
import com.dms.domain.UserGroup;
import com.dms.exception.TenantMismatchException;
import com.dms.exception.UnauthorizedAccessException;
import com.dms.repository.GroupRepository;
import com.dms.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final TenantContext tenantContext;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;

    public void assertCanAccessDocument(Document document) {
        if (!canAccessDocument(document)) {
            throw new UnauthorizedAccessException("User not authorized for document");
        }
    }

    public boolean canAccessDocument(Document document) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();

        if (!tenantId.equals(document.getTenantId())) {
            throw new TenantMismatchException("Cross-tenant access is not allowed");
        }

        UUID[] allowedGroups = document.getDocumentType().getAllowedGroups();
        if (allowedGroups == null || allowedGroups.length == 0) {
            return true;
        }

        Set<UUID> effectiveGroupIds = resolveEffectiveUserGroups(tenantId, userId);
        return Arrays.stream(allowedGroups).anyMatch(effectiveGroupIds::contains);
    }

    public boolean canUploadToType(UUID[] allowedGroups) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        String userId = tenantContext.getCurrentUserId();
        if (allowedGroups == null || allowedGroups.length == 0) {
            return true;
        }
        Set<UUID> effectiveGroupIds = resolveEffectiveUserGroups(tenantId, userId);
        return Arrays.stream(allowedGroups).anyMatch(effectiveGroupIds::contains);
    }

    public boolean canDeleteDocument(Document document) {
        return canAccessDocument(document) && (hasRole("ADMIN") || hasRole("DOCUMENT_ADMIN"));
    }

    public boolean canViewAuditLogs() {
        return hasRole("ADMIN") || hasRole("COMPLIANCE_OFFICER");
    }

    Set<UUID> resolveEffectiveUserGroups(UUID tenantId, String userId) {
        List<UserGroup> assignments = userGroupRepository.findAllByTenantIdAndUserId(tenantId, userId);
        Set<UUID> directGroupIds = assignments.stream().map(ug -> ug.getGroup().getId()).collect(java.util.stream.Collectors.toSet());

        List<Group> tenantGroups = groupRepository.findAllByTenantId(tenantId);
        Map<UUID, UUID> parentByChild = new HashMap<>();
        for (Group group : tenantGroups) {
            if (group.getParentGroup() != null) {
                parentByChild.put(group.getId(), group.getParentGroup().getId());
            }
        }

        Set<UUID> effective = new HashSet<>(directGroupIds);
        ArrayDeque<UUID> stack = new ArrayDeque<>(directGroupIds);
        while (!stack.isEmpty()) {
            UUID current = stack.pop();
            UUID parent = parentByChild.get(current);
            if (parent != null && effective.add(parent)) {
                stack.push(parent);
            }
        }

        return effective;
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String expected = "ROLE_" + role;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
