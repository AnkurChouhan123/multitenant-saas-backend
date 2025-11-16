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
 * JWT Authentication Filter
 * Validates JWT tokens and sets security context
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
                        
                        // Create authentication token
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                email, 
                                null, 
                                Collections.singletonList(authority)
                            );
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // Set security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("JWT authentication successful for user: {} (tenant: {})", email, tenantId);
                    } else {
                        log.warn("Invalid JWT token for email: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
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
        // Don't filter auth endpoints
        return path.startsWith("/api/auth/");
    }
}