package com.saas.platform.repository;

import com.saas.platform.model.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TenantSettingsRepository - Database operations for TenantSettings
 */
@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {
    
    /**
     * Find settings by tenant ID
     */
    Optional<TenantSettings> findByTenantId(Long tenantId);
    
    /**
     * Check if settings exist for tenant
     */
    boolean existsByTenantId(Long tenantId);
    
    /**
     * Delete settings by tenant ID
     */
    void deleteByTenantId(Long tenantId);
}