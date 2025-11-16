package com.saas.platform.controller;

import com.saas.platform.dto.ApiKeyCreateRequest;
import com.saas.platform.dto.ApiKeyResponse;
import com.saas.platform.model.ApiKey;
import com.saas.platform.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key Management Controller
 * Allows tenants to create and manage API keys for external integrations
 */
@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "http://localhost:3000")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    /**
     * GET /api/keys/tenant/{tenantId} - Get all API keys for tenant
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<ApiKeyResponse>> getTenantApiKeys(@PathVariable Long tenantId) {
        List<ApiKey> apiKeys = apiKeyService.getApiKeysByTenant(tenantId);
        List<ApiKeyResponse> response = apiKeys.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/keys/tenant/{tenantId} - Create new API key
     */
    @PostMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @PathVariable Long tenantId,
            @RequestBody ApiKeyCreateRequest request) {
        
        ApiKey apiKey = apiKeyService.createApiKey(
            tenantId, 
            request.getUserId(),
            request.getName(),
            request.getScopes(),
            request.getExpiresInDays()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(apiKey));
    }
    
    /**
     * PUT /api/keys/{keyId}/revoke - Revoke API key
     */
    @PutMapping("/{keyId}/revoke")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> revokeApiKey(@PathVariable Long keyId) {
        apiKeyService.revokeApiKey(keyId);
        return ResponseEntity.ok("API key revoked successfully");
    }
    
    /**
     * DELETE /api/keys/{keyId} - Delete API key
     */
    @DeleteMapping("/{keyId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long keyId) {
        apiKeyService.deleteApiKey(keyId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * GET /api/keys/{keyId}/usage - Get API key usage statistics
     */
    @GetMapping("/{keyId}/usage")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiKeyResponse> getApiKeyUsage(@PathVariable Long keyId) {
        ApiKey apiKey = apiKeyService.getApiKeyById(keyId);
        return ResponseEntity.ok(convertToResponse(apiKey));
    }
    
    private ApiKeyResponse convertToResponse(ApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setName(apiKey.getName());
        response.setKeyValue(apiKey.getKeyValue());
        response.setTenantId(apiKey.getTenantId());
        response.setIsActive(apiKey.getIsActive());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setLastUsedAt(apiKey.getLastUsedAt());
        response.setUsageCount(apiKey.getUsageCount());
        response.setRateLimitPerHour(apiKey.getRateLimitPerHour());
        response.setScopes(apiKey.getScopes());
        response.setCreatedAt(apiKey.getCreatedAt());
        return response;
    }
}