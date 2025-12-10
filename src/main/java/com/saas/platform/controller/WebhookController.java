package com.saas.platform.controller;

import com.saas.platform.dto.WebhookCreateRequest;
import com.saas.platform.model.Webhook;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WebhookController - UPDATED
 * Only TENANT_OWNER and TENANT_ADMIN can manage webhooks
 * SUPER_ADMIN, USER, and VIEWER are NOT allowed
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "http://localhost:3000")
public class WebhookController {
    
    private final WebhookService webhookService;
    private final RoleValidator roleValidator;
    
    public WebhookController(WebhookService webhookService, RoleValidator roleValidator) {
        super();
        this.webhookService = webhookService;
        this.roleValidator = roleValidator;
    }
    
    
    /**
     * Get all webhooks for tenant
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Webhook>> getTenantWebhooks(@PathVariable Long tenantId) {
        roleValidator.requireWebhookPermission(tenantId);
        List<Webhook> webhooks = webhookService.getWebhooksByTenant(tenantId);
        return ResponseEntity.ok(webhooks);
    }
    
    
    /**
     * Create new webhook
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @PostMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Webhook> createWebhook(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestBody WebhookCreateRequest request) {
        
        roleValidator.requireWebhookPermission(tenantId);
        
        Webhook webhook = webhookService.createWebhook(
            tenantId,
            request.getName(),
            request.getUrl(),
            request.getEvents(),
            userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(webhook);
    }
    
    /**
     * Update webhook
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @PutMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Webhook> updateWebhook(
            @PathVariable Long webhookId,
            @RequestBody WebhookCreateRequest request) {
        
        Webhook webhook = webhookService.updateWebhook(
            webhookId,
            request.getName(),
            request.getUrl(),
            request.getEvents(),
            request.getIsActive()
        );
        
        return ResponseEntity.ok(webhook);
    }
    
    /**
     * Delete webhook
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @DeleteMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long webhookId) {
        webhookService.deleteWebhook(webhookId);
        return ResponseEntity.noContent().build();
    }
    
    
    /**
     * Test webhook by sending ping
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @PostMapping("/{webhookId}/test")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<String> testWebhook(@PathVariable Long webhookId) {
        boolean success = webhookService.testWebhook(webhookId);
        
        if (success) {
            return ResponseEntity.ok("Webhook test successful");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Webhook test failed");
        }
    }
    
    
    /**
     * Get webhook statistics
     * Only TENANT_OWNER and TENANT_ADMIN
     */
    @GetMapping("/{webhookId}/stats")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<WebhookService.WebhookStats> getWebhookStats(
            @PathVariable Long webhookId) {
        
        WebhookService.WebhookStats stats = webhookService.getWebhookStats(webhookId);
        return ResponseEntity.ok(stats);
    }
}