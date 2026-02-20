package com.dms.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTenantIdFromTenantIdClaim() {
        TenantContext tenantContext = new TenantContext(DEFAULT_TENANT);
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claims(claims -> claims.putAll(Map.of("tenant_id", tenantId.toString())))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(tenantContext.getCurrentTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldFallbackToTidClaimWhenTenantIdMissing() {
        TenantContext tenantContext = new TenantContext(DEFAULT_TENANT);
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claims(claims -> claims.putAll(Map.of("tid", tenantId.toString())))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(tenantContext.getCurrentTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldReturnDefaultTenantWhenClaimIsInvalid() {
        TenantContext tenantContext = new TenantContext(DEFAULT_TENANT);
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claims(claims -> claims.putAll(Map.of("tenant_id", "not-a-uuid")))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(tenantContext.getCurrentTenantId()).isEqualTo(DEFAULT_TENANT);
    }
}
