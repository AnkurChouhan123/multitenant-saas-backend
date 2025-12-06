
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

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "http://localhost:3000")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    // VIEWER CAN VIEW API KEYS (but not modify)
     
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<ApiKeyResponse>> getTenantApiKeys(@PathVariable Long tenantId) {
        List<ApiKey> apiKeys = apiKeyService.getApiKeysByTenant(tenantId);
        List<ApiKeyResponse> response = apiKeys.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
  
     // Anyone can create api keys (except viewer and user)
    
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
    
   // revoke or cancel or invalidate api key (only tenant admin and super admin)
    @PutMapping("/{keyId}/revoke")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> revokeApiKey(@PathVariable Long keyId) {
        apiKeyService.revokeApiKey(keyId);
        return ResponseEntity.ok("API key revoked successfully");
    }
    
    // delete api keys
    @DeleteMapping("/{keyId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long keyId) {
        apiKeyService.deleteApiKey(keyId);
        return ResponseEntity.noContent().build();
    }
    
   // view usage of api key
    @GetMapping("/{keyId}/usage")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
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
