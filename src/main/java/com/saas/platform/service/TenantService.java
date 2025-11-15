package com.saas.platform.service;

import com.saas.platform.model.Tenant;
import com.saas.platform.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TenantService - Business logic for tenant operations
 */
@Service
public class TenantService {
    
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    
    private final TenantRepository tenantRepository;
    
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("Creating new tenant: {}", tenant.getName());
        
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + tenant.getSubdomain());
        }
        
        tenant.setDatabaseName("tenant_" + tenant.getSubdomain());
        
        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Tenant created successfully with ID: {}", savedTenant.getId());
        
        return savedTenant;
    }
    
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }
    
    public Tenant getTenantById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with ID: " + id));
    }
    
    public Tenant getTenantBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with subdomain: " + subdomain));
    }
    
    @Transactional
    public Tenant updateTenant(Long id, Tenant tenantDetails) {
        Tenant tenant = getTenantById(id);
        
        tenant.setName(tenantDetails.getName());
        tenant.setStatus(tenantDetails.getStatus());
        
        return tenantRepository.save(tenant);
    }
    
    @Transactional
    public void deleteTenant(Long id) {
        Tenant tenant = getTenantById(id);
        tenantRepository.delete(tenant);
        log.info("Tenant deleted: {}", tenant.getName());
    }
}