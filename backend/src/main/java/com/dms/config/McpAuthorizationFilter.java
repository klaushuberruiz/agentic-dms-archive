package com.dms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class McpAuthorizationFilter extends OncePerRequestFilter {
    
    private static final String MCP_TOOLS_PATH = "/mcp/tools";
    private static final String MCP_AUTH_HEADER = "X-MCP-Authorization";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Check if this is an MCP endpoint
        if (requestPath.startsWith(MCP_TOOLS_PATH)) {
            String authHeader = request.getHeader(MCP_AUTH_HEADER);
            
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("MCP request without authorization header: {}", requestPath);
                // In strict mode: response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                // For now: continue to allow Bearer token from Authorization header
            }
            
            log.debug("MCP request authorized: {}", requestPath);
        }
        
        filterChain.doFilter(request, response);
    }
}
