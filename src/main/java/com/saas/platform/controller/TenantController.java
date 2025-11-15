package com.saas.platform.controller;

import com.saas.platform.model.Tenant;
import com.saas.platform.service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TenantController - REST API for tenant management
 */
@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = "http://localhost:3000")
public class TenantController {
    
    private final TenantService tenantService;
    
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable Long id) {
        Tenant tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }
    
    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<Tenant> getTenantBySubdomain(@PathVariable String subdomain) {
        Tenant tenant = tenantService.getTenantBySubdomain(subdomain);
        return ResponseEntity.ok(tenant);
    }
    
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant createdTenant = tenantService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        Tenant updatedTenant = tenantService.updateTenant(id, tenant);
        return ResponseEntity.ok(updatedTenant);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}