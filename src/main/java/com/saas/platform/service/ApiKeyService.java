package com.saas.platform.service;

import com.saas.platform.model.ApiKey;
import com.saas.platform.model.User;
import com.saas.platform.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//
// ApiKeyService - Business logic for API Key management
// Handles creation, validation, and management of API keys for tenant integrations
 
@Service
public class ApiKeyService {
    
    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    
    private final ApiKeyRepository apiKeyRepository;
    private final ActivityLogService activityLogService;
    private final UserService userService;
    
    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                        ActivityLogService activityLogService,
                        UserService userService) {
        this.apiKeyRepository = apiKeyRepository;
        this.activityLogService = activityLogService;
        this.userService = userService;
    }
    
    //
// Create a new API key for a tenant
     
    @Transactional
    public ApiKey createApiKey(Long tenantId, Long userId, String name, 
                               String scopes, Integer expiresInDays) {
        log.info("Creating API key '{}' for tenant ID: {}", name, tenantId);
        
        // Validate user belongs to tenant
        User user = userService.getUserById(userId);
        if (!user.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to this tenant");
        }
        
        // Create API key
        ApiKey apiKey = new ApiKey(name, tenantId, userId);
        
        if (scopes != null && !scopes.isEmpty()) {
            apiKey.setScopes(scopes);
        } else {
            apiKey.setScopes("read,write"); // Default scopes
        }
        
        // Set expiration if specified
        if (expiresInDays != null && expiresInDays > 0) {
            apiKey.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }
        
        ApiKey savedKey = apiKeyRepository.save(apiKey);
        
        // Log activity
        activityLogService.logActivity(
            tenantId,
            userId,
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "API Key created: " + name,
            "security",
            "New API key generated with scopes: " + scopes
        );
        
        log.info("API key created successfully with ID: {}", savedKey.getId());
        
        return savedKey;
    }
    
    //
// Get all API keys for a tenant
     
    public List<ApiKey> getApiKeysByTenant(Long tenantId) {
        return apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
    
    //
// Get API key by ID
     
    public ApiKey getApiKeyById(Long keyId) {
        return apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found with ID: " + keyId));
    }
    
    //
// Get API key by key value (for authentication)
     
    public ApiKey getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));
    }
    
    //
// Validate API key and check permissions
     
    public boolean validateApiKey(String keyValue, String requiredScope) {
        try {
            ApiKey apiKey = getApiKeyByValue(keyValue);
            
            // Check if key is active
            if (!apiKey.getIsActive()) {
                log.warn("Inactive API key attempted: {}", keyValue);
                return false;
            }
            
            // Check if key is expired
            if (apiKey.isExpired()) {
                log.warn("Expired API key attempted: {}", keyValue);
                return false;
            }
            
            // Check if key has required scope
            if (requiredScope != null && !apiKey.hasScope(requiredScope)) {
                log.warn("API key lacks required scope '{}': {}", requiredScope, keyValue);
                return false;
            }
            
            // Record usage
            recordApiKeyUsage(apiKey.getId());
            
            return true;
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid API key validation attempt");
            return false;
        }
    }
    
    //
// Record API key usage
     
    @Transactional
    public void recordApiKeyUsage(Long keyId) {
        ApiKey apiKey = getApiKeyById(keyId);
        apiKey.incrementUsage();
        apiKeyRepository.save(apiKey);
    }
    
    //
// Revoke (deactivate) an API key
     
    @Transactional
    public void revokeApiKey(Long keyId) {
        log.info("Revoking API key ID: {}", keyId);
        
        ApiKey apiKey = getApiKeyById(keyId);
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        
        // Log activity
        activityLogService.logActivity(
            apiKey.getTenantId(),
            apiKey.getCreatedBy(),
            "system",
            "System",
            "API Key revoked: " + apiKey.getName(),
            "security",
            "API key has been deactivated"
        );
        
        log.info("API key revoked successfully");
    }
    
    //
// Delete an API key permanently
     
    @Transactional
    public void deleteApiKey(Long keyId) {
        log.info("Deleting API key ID: {}", keyId);
        
        ApiKey apiKey = getApiKeyById(keyId);
        Long tenantId = apiKey.getTenantId();
        String name = apiKey.getName();
        
        apiKeyRepository.delete(apiKey);
        
        // Log activity
        activityLogService.logActivity(
            tenantId,
            apiKey.getCreatedBy(),
            "system",
            "System",
            "API Key deleted: " + name,
            "security",
            "API key has been permanently removed"
        );
        
        log.info("API key deleted successfully");
    }
    
    //
// Get active API keys count for tenant
     
    public long getActiveApiKeysCount(Long tenantId) {
        return apiKeyRepository.countByTenantIdAndIsActiveTrue(tenantId);
    }
    
    //
// Check if tenant has reached API key limit
     
    public boolean hasReachedApiKeyLimit(Long tenantId, int maxKeys) {
        long activeKeys = getActiveApiKeysCount(tenantId);
        return activeKeys >= maxKeys;
    }
    
    //
// Rotate API key (generate new key, revoke old)
     
    @Transactional
    public ApiKey rotateApiKey(Long keyId, Long userId) {
        log.info("Rotating API key ID: {}", keyId);
        
        ApiKey oldKey = getApiKeyById(keyId);
        
        // Create new key with same settings
        ApiKey newKey = createApiKey(
            oldKey.getTenantId(),
            userId,
            oldKey.getName() + " (Rotated)",
            oldKey.getScopes(),
            null // No expiration by default
        );
        
        // Revoke old key
        revokeApiKey(keyId);
        
        log.info("API key rotated successfully");
        
        return newKey;
    }
    
    //
// Update API key settings
     
    @Transactional
    public ApiKey updateApiKey(Long keyId, String name, String scopes, 
                               Integer rateLimitPerHour, String allowedIps) {
        log.info("Updating API key ID: {}", keyId);
        
        ApiKey apiKey = getApiKeyById(keyId);
        
        if (name != null && !name.isEmpty()) {
            apiKey.setName(name);
        }
        
        if (scopes != null) {
            apiKey.setScopes(scopes);
        }
        
        if (rateLimitPerHour != null) {
            apiKey.setRateLimitPerHour(rateLimitPerHour);
        }
        
        if (allowedIps != null) {
            apiKey.setAllowedIps(allowedIps);
        }
        
        ApiKey updated = apiKeyRepository.save(apiKey);
        
        // Log activity
        activityLogService.logActivity(
            apiKey.getTenantId(),
            apiKey.getCreatedBy(),
            "system",
            "System",
            "API Key updated: " + apiKey.getName(),
            "security",
            "API key settings have been modified"
        );
        
        log.info("API key updated successfully");
        
        return updated;
    }
}