package com.dms.service;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantContextProperties {

    @Property
    void invalidTenantClaimFallsBackToDefault(@ForAll("nonUuid") String invalidTenant) {
        UUID defaultTenant = UUID.fromString("00000000-0000-0000-0000-000000000001");
        TenantContext context = new TenantContext(defaultTenant);

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), Map.of("tenant_id", invalidTenant));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertEquals(defaultTenant, context.getCurrentTenantId());
        SecurityContextHolder.clearContext();
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> nonUuid() {
        return net.jqwik.api.Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }
}
