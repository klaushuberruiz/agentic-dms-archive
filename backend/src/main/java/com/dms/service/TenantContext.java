package com.dms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantContext {

    private final UUID defaultTenantId;

    public TenantContext(@Value("${dms.security.default-tenant-id:00000000-0000-0000-0000-000000000001}") UUID defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String tenantId = jwtAuth.getToken().getClaimAsString("tenant_id");
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = jwtAuth.getToken().getClaimAsString("tid");
            }
            if (tenantId != null && !tenantId.isBlank()) {
                try {
                    return UUID.fromString(tenantId);
                } catch (IllegalArgumentException ignored) {
                    // fall back to configured default tenant when claim format is invalid
                }
            }
        }

        return defaultTenantId;
    }

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
