package com.saas.platform.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(0)
public class RequestLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        long startTime = System.currentTimeMillis();
        
        log.info("API Request: {} {} from {}", 
            httpRequest.getMethod(), 
            httpRequest.getRequestURI(), 
            request.getRemoteAddr());
        
        chain.doFilter(request, response);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("API Response: {} {} completed in {}ms", 
            httpRequest.getMethod(), 
            httpRequest.getRequestURI(), 
            duration);
    }
}