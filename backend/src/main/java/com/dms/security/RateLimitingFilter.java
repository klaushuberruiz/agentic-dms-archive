package com.dms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${dms.rate-limit.requests-per-minute:100}")
    private int maxRequestsPerMinute;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String key = resolveKey();
        long minute = Instant.now().getEpochSecond() / 60;

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != minute) {
                return new WindowCounter(minute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > maxRequestsPerMinute) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "errorCode", "RATE_LIMIT_EXCEEDED",
                "message", "Rate limit exceeded"
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication != null ? authentication.getName() : "anonymous";
        String tenant = "default";
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String tenantId = jwtAuth.getToken().getClaimAsString("tenant_id");
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = jwtAuth.getToken().getClaimAsString("tid");
            }
            if (tenantId != null && !tenantId.isBlank()) {
                tenant = tenantId;
            }
        }
        return user + ":" + tenant;
    }

    private record WindowCounter(long minute, AtomicInteger count) {
    }
}
