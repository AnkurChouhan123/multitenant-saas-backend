package com.saas.platform.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TenantFilter - Extracts tenant identifier from each HTTP request
 * This runs BEFORE any controller logic
 */
@Component
@Order(1)
public class TenantFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            String tenantId = resolveTenantIdentifier(httpRequest);
            
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setCurrentTenant(tenantId);
                log.debug("Tenant context set to: {}", tenantId);
            } else {
                log.debug("No tenant identifier found, using default");
                TenantContext.setCurrentTenant("public");
            }
            
            // Continue with the request
            chain.doFilter(request, response);
            
        } finally {
            // ALWAYS clear context after request completes
            TenantContext.clear();
            log.debug("Tenant context cleared");
        }
    }
    
    /**
     * Extract tenant ID from request using multiple strategies
     */
    private String resolveTenantIdentifier(HttpServletRequest request) {
        
        // Strategy 1: Check X-Tenant-ID header (for API requests)
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            log.debug("Tenant resolved from header: {}", tenantId);
            return tenantId;
        }
        
        // Strategy 2: Extract from subdomain (e.g., acme.platform.com)
        String serverName = request.getServerName();
        if (serverName != null && serverName.contains(".")) {
            String[] parts = serverName.split("\\.");
            if (parts.length >= 2) {
                String subdomain = parts[0];
                // Skip common subdomains
                if (!subdomain.equals("www") && !subdomain.equals("api") && !subdomain.equals("localhost")) {
                    log.debug("Tenant resolved from subdomain: {}", subdomain);
                    return subdomain;
                }
            }
        }
        
        // Strategy 3: Extract from URL path (e.g., /api/tenant/acme/users)
        String path = request.getRequestURI();
        if (path != null && path.contains("/tenant/")) {
            String[] pathParts = path.split("/");
            for (int i = 0; i < pathParts.length - 1; i++) {
                if (pathParts[i].equals("tenant")) {
                    log.debug("Tenant resolved from path: {}", pathParts[i + 1]);
                    return pathParts[i + 1];
                }
            }
        }
        
        log.debug("No tenant identifier found in request");
        return null;
    }
}