package com.saas.platform.service;

import com.saas.platform.dto.AuthResponse;
import com.saas.platform.dto.LoginRequest;
import com.saas.platform.dto.RegisterRequest;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.User;
import com.saas.platform.model.UserRole;
import com.saas.platform.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final UserService userService;
    private final TenantService tenantService;
    private final JwtUtil jwtUtil;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;
    
    public AuthService(UserService userService,
    		           TenantService tenantService, 
                      JwtUtil jwtUtil,
                      ActivityLogService activityLogService,
                      EmailService emailService) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.jwtUtil = jwtUtil;
        this.activityLogService = activityLogService;
        this.emailService = emailService;
    }
    
    //
// Login - Authenticate user and return JWT token
     
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        // Find user by email
        User user = userService.getUserByEmail(request.getEmail());
        
        // Verify password
        if (!userService.verifyPassword(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        // Check if user is active
        if (!user.getActive()) {
            log.warn("Inactive user attempted login: {}", request.getEmail());
            throw new IllegalArgumentException("User account is inactive");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getTenant().getId(),
                user.getRole().toString()
        );
        
        // Log activity
        activityLogService.logActivity(
            user.getTenant().getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "Logged in",
            "auth",
            "User successfully authenticated"
        );
        
        log.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());
        
        // Build response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().toString());
        response.setTenantId(user.getTenant().getId());
        response.setTenantName(user.getTenant().getName());
        response.setSubdomain(user.getTenant().getSubdomain());
        
        return response;
    }
    
    //
// Register - Create new tenant and TENANT_OWNER user
// UPDATED: First user is now TENANT_OWNER instead of TENANT_ADMIN
     
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("═══════════════════════════════════════");
        log.info("📝 REGISTRATION STARTED");
        log.info("  Email: {}", request.getEmail());
        log.info("  Company: {}", request.getCompanyName());
        log.info("═══════════════════════════════════════");
        
        // Create tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.getCompanyName());
        tenant.setSubdomain(request.getSubdomain());
        Tenant savedTenant = tenantService.createTenant(tenant);
        log.info("✅ Tenant created: {} (ID: {})", savedTenant.getName(), savedTenant.getId());
        
        // Create TENANT_OWNER user (first user of tenant)
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(UserRole.TENANT_OWNER); // ✅ CHANGED: First user is TENANT_OWNER
        
        User savedUser = userService.createUser(user, savedTenant.getId());
        log.info("✅ TENANT_OWNER created: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser, savedTenant.getName());
            log.info("✅ Welcome email sent");
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email: {}", e.getMessage());
            // Don't fail registration if email fails
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedTenant.getId(),
                savedUser.getRole().toString()
        );
        
        // Log activity
        activityLogService.logActivity(
            savedTenant.getId(),
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName() + " " + savedUser.getLastName(),
            "Account registered as TENANT_OWNER",
            "auth",
            "New tenant and owner user created"
        );
        
        log.info("═══════════════════════════════════════");
        log.info("✅ REGISTRATION COMPLETED");
        log.info("  User: {} (TENANT_OWNER)", savedUser.getEmail());
        log.info("  Tenant: {}", savedTenant.getName());
        log.info("═══════════════════════════════════════");
        
        // Build response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(savedUser.getId());
        response.setEmail(savedUser.getEmail());
        response.setFirstName(savedUser.getFirstName());
        response.setLastName(savedUser.getLastName());
        response.setRole(savedUser.getRole().toString());
        response.setTenantId(savedTenant.getId());
        response.setTenantName(savedTenant.getName());
        response.setSubdomain(savedTenant.getSubdomain());
        
        return response;
    }
}