package com.saas.platform.controller;

import com.saas.platform.dto.WebhookCreateRequest;
import com.saas.platform.model.Webhook;
import com.saas.platform.service.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WebhookController - REST API for webhook management
 * Allows tenants to configure webhooks for receiving event notifications
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "http://localhost:3000")
public class WebhookController {
    
    private final WebhookService webhookService;
    
    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    /**
     * GET /api/webhooks/tenant/{tenantId} - Get all webhooks for tenant
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Webhook>> getTenantWebhooks(@PathVariable Long tenantId) {
        List<Webhook> webhooks = webhookService.getWebhooksByTenant(tenantId);
        return ResponseEntity.ok(webhooks);
    }
    
    /**
     * POST /api/webhooks/tenant/{tenantId} - Create new webhook
     */
    @PostMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Webhook> createWebhook(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestBody WebhookCreateRequest request) {
        
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
     * PUT /api/webhooks/{webhookId} - Update webhook
     */
    @PutMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
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
     * DELETE /api/webhooks/{webhookId} - Delete webhook
     */
    @DeleteMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long webhookId) {
        webhookService.deleteWebhook(webhookId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * POST /api/webhooks/{webhookId}/test - Test webhook by sending ping
     */
    @PostMapping("/{webhookId}/test")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
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
     * GET /api/webhooks/{webhookId}/stats - Get webhook statistics
     */
    @GetMapping("/{webhookId}/stats")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<WebhookService.WebhookStats> getWebhookStats(
            @PathVariable Long webhookId) {
        
        WebhookService.WebhookStats stats = webhookService.getWebhookStats(webhookId);
        return ResponseEntity.ok(stats);
    }
}