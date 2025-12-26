
package com.saas.platform.security;

import java.lang.annotation.*;

//
// Custom annotation for file operations
 
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFilePermission {
    FileOperation value();
    
    enum FileOperation {
        UPLOAD,
        DOWNLOAD,
        DELETE,
        PERMANENT_DELETE,
        SHARE
    }
}


