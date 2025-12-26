
package com.saas.platform.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionCheckAspect {
    
    private final RoleValidator roleValidator;
    
    public PermissionCheckAspect(RoleValidator roleValidator) {
        this.roleValidator = roleValidator;
    }
    
    //
// Check file permissions before method execution
     
    @Before("@annotation(com.saas.platform.security.RequireFilePermission)")
    public void checkFilePermission(JoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequireFilePermission annotation = method.getAnnotation(RequireFilePermission.class);
        RequireFilePermission.FileOperation operation = annotation.value();
        
        switch (operation) {
            case UPLOAD:
                roleValidator.requireUploadPermission();
                break;
            case PERMANENT_DELETE:
                // Extract tenantId from method arguments
                Object[] args = joinPoint.getArgs();
                Long tenantId = extractTenantId(args);
                roleValidator.requirePermanentDeletePermission(tenantId);
                break;
            // Add other cases as needed
        }
    }
    
    //
// Check user permissions before method execution
     
    @Before("@annotation(com.saas.platform.security.RequireUserPermission)")
    public void checkUserPermission(JoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequireUserPermission annotation = method.getAnnotation(RequireUserPermission.class);
        RequireUserPermission.UserOperation operation = annotation.value();
        
        Object[] args = joinPoint.getArgs();
        Long tenantId = extractTenantId(args);
        
        switch (operation) {
            case CREATE:
            case UPDATE:
            case DELETE:
            case VIEW_ALL:
            case MANAGE_ROLES:
                roleValidator.requireUserManagementPermission(tenantId);
                break;
        }
    }
    
    private Long extractTenantId(Object[] args) {
        // Logic to extract tenantId from method arguments
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }
}
