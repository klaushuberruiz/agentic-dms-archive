package com.dms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class McpAuthorizationFilter extends OncePerRequestFilter {

    private static final String MCP_TOOLS_PATH = "/mcp/tools";
    private static final String MCP_AUTH_HEADER = "X-MCP-Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        if (requestPath.startsWith(MCP_TOOLS_PATH)) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String mcpAuthHeader = request.getHeader(MCP_AUTH_HEADER);
            boolean bearerPresent = authHeader != null && authHeader.startsWith("Bearer ");
            boolean mcpBearerPresent = mcpAuthHeader != null && mcpAuthHeader.startsWith("Bearer ");

            if (!bearerPresent && !mcpBearerPresent) {
                log.warn("Rejected MCP request without bearer token: {}", requestPath);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
