
package com.saas.platform.controller;

import com.saas.platform.model.Tenant;
import com.saas.platform.security.RoleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saas.platform.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TenantController {
    
    private static final Logger log = LoggerFactory.getLogger(TenantController.class);
    
    private final TenantService tenantService;
    private final RoleValidator roleValidator;
    
    public TenantController(TenantService tenantService, RoleValidator roleValidator) {
        this.tenantService = tenantService;
        this.roleValidator = roleValidator;
    }
    
   // Get all tenants - SUPER_ADMIN only
     
    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        log.info("🔍 SUPER_ADMIN fetching all tenants");
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }
    
   // Get tenant by ID - Any authenticated user can view their tenant
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable Long id) {
        log.info("🔍 Fetching tenant by ID: {}", id);
        Tenant tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }
    
   // Get tenant by subdomain - Public for login purposes
    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<Tenant> getTenantBySubdomain(@PathVariable String subdomain) {
        log.info("🔍 Fetching tenant by subdomain: {}", subdomain);
        Tenant tenant = tenantService.getTenantBySubdomain(subdomain);
        return ResponseEntity.ok(tenant);
    }
    
    // Create tenant - Handled by registration, SUPER_ADMIN only for direct creation
    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        log.info("➕ SUPER_ADMIN creating new tenant: {}", tenant.getName());
        Tenant createdTenant = tenantService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
    }
    
    
     // Update tenant - ONLY TENANT_OWNER or SUPER_ADMIN
     //🔒 CRITICAL: This is where company name/settings are changed
     
    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable Long id, 
            @RequestBody Tenant tenant) {
        
        log.info("═══════════════════════════════");
        log.info("🔐 UPDATE TENANT REQUEST");
        log.info("  Tenant ID: {}", id);
        log.info("  New Name: {}", tenant.getName());
        log.info("  User Role: {}", roleValidator.getCurrentUser().getRole());
        log.info("═══════════════════════════════");
        
        // ✅ CRITICAL SECURITY CHECK
        roleValidator.requireTenantSettingsPermission(id);
        
        Tenant updatedTenant = tenantService.updateTenant(id, tenant);
        
        log.info("✅ Tenant updated successfully by authorized user");
        log.info("  Saved Name: {}", updatedTenant.getName());
        log.info("═══════════════════════════════");
        
        return ResponseEntity.ok(updatedTenant);
    }
    
//    Delete tenant - SUPER_ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        log.info("🗑️ SUPER_ADMIN deleting tenant: {}", id);
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}

