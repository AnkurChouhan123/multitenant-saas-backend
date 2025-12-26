package com.saas.platform.repository;

import com.saas.platform.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//
// ApiKeyRepository - Database operations for API Keys
 
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    //
// Find API key by its value
     
    Optional<ApiKey> findByKeyValue(String keyValue);
    
    //
// Find all API keys for a tenant
     
    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    
    //
// Find active API keys for a tenant
     
    List<ApiKey> findByTenantIdAndIsActiveTrue(Long tenantId);
    
    //
// Count active API keys for a tenant
     
    long countByTenantIdAndIsActiveTrue(Long tenantId);
    
    //
// Find API keys created by a specific user
     
    List<ApiKey> findByCreatedBy(Long userId);
    
    //
// Find expired API keys
     
    List<ApiKey> findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime dateTime);
    
    //
// Check if API key exists by value
     
    boolean existsByKeyValue(String keyValue);
}