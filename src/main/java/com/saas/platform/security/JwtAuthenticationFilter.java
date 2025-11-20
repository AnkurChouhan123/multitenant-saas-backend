package com.saas.platform.security;

import com.saas.platform.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter - FIXED
 * Fixed issues:
 * 1. Better error handling
 * 2. Clear authentication on invalid tokens
 * 3. Proper role prefix handling
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && !token.isEmpty()) {
                try {
                    String email = jwtUtil.extractEmail(token);
                    
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        
                        if (jwtUtil.validateToken(token, email)) {
                            // Extract user details from token
                            Long userId = jwtUtil.extractUserId(token);
                            Long tenantId = jwtUtil.extractTenantId(token);
                            String role = jwtUtil.extractRole(token);
                            
                            // Set tenant context
                            if (tenantId != null) {
                                TenantContext.setCurrentTenant(tenantId.toString());
                                log.debug("Set tenant context: {}", tenantId);
                            }
                            
                            // FIXED: Ensure role has proper prefix
                            String roleWithPrefix = role;
                            if (!role.startsWith("ROLE_")) {
                                roleWithPrefix = "ROLE_" + role;
                            }
                            
                            // Create authentication token
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleWithPrefix);

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            email,
                                            null,
                                            Collections.singletonList(authority) // authorities already set = authenticated
                                    );

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            
                            log.debug("JWT authentication successful for user: {} with role: {} (tenant: {})", 
                                     email, roleWithPrefix, tenantId);
                        } else {
                            log.warn("Invalid JWT token for email: {}", email);
                        }
                    }
                } catch (Exception e) {
                    log.error("JWT token validation error: {}", e.getMessage());
                    // Clear the security context on invalid token
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter auth endpoints and error endpoint
        return path.startsWith("/api/auth/") || path.equals("/error");
    }
}