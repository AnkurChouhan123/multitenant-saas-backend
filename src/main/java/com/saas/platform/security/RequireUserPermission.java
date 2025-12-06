package com.saas.platform.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireUserPermission {
    UserOperation value();
    
    enum UserOperation {
        CREATE,
        UPDATE,
        DELETE,
        VIEW_ALL,
        MANAGE_ROLES
    }
}
