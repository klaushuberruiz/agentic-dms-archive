package com.dms.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantContext {
    
    public UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Extract tenant from JWT claims
        // For now, return a default tenant for development
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
    
    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
