package com.dms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTooManyRequestsWhenLimitExceeded() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "maxRequestsPerMinute", 1);

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("tenant_id", "00000000-0000-0000-0000-000000000001")
            .subject("user-1")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/documents");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request1, response1, new MockFilterChain());
        assertThat(response1.getStatus()).isEqualTo(200);

        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/v1/documents");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, new MockFilterChain());

        assertThat(response2.getStatus()).isEqualTo(429);
        assertThat(response2.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response2.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }
}
