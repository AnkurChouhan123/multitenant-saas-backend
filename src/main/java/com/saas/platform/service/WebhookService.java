package com.saas.platform.service;

import com.saas.platform.model.Webhook;
import com.saas.platform.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * WebhookService - FIXED
 * Fixed: RestTemplate now injected via constructor instead of new instance
 */
@Service
public class WebhookService {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    
    private final WebhookRepository webhookRepository;
    private final ActivityLogService activityLogService;
    private final RestTemplate restTemplate;
    
    /**
     * ✅ FIXED: RestTemplate injected via constructor
     */
    public WebhookService(WebhookRepository webhookRepository,
                         ActivityLogService activityLogService,
                         RestTemplate restTemplate) {
        this.webhookRepository = webhookRepository;
        this.activityLogService = activityLogService;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Create a new webhook
     */
    @Transactional
    public Webhook createWebhook(Long tenantId, String name, String url, 
                                 String events, Long userId) {
        log.info("Creating webhook '{}' for tenant ID: {}", name, tenantId);
        
        // Generate secret key for signature verification
        String secretKey = generateSecretKey();
        
        Webhook webhook = new Webhook(tenantId, name, url, events);
        webhook.setSecretKey(secretKey);
        
        Webhook saved = webhookRepository.save(webhook);
        
        // Log activity
        activityLogService.logActivity(
            tenantId,
            userId,
            "system",
            "System",
            "Webhook created: " + name,
            "settings",
            "Webhook configured for events: " + events
        );
        
        log.info("Webhook created successfully with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Get all webhooks for a tenant
     */
    public List<Webhook> getWebhooksByTenant(Long tenantId) {
        return webhookRepository.findByTenantId(tenantId);
    }
    
    /**
     * Get active webhooks for a tenant
     */
    public List<Webhook> getActiveWebhooks(Long tenantId) {
        return webhookRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }
    
    /**
     * Get webhook by ID
     */
    public Webhook getWebhookById(Long webhookId) {
        return webhookRepository.findById(webhookId)
            .orElseThrow(() -> new IllegalArgumentException("Webhook not found with ID: " + webhookId));
    }
    
    /**
     * Update webhook
     */
    @Transactional
    public Webhook updateWebhook(Long webhookId, String name, String url, 
                                 String events, Boolean isActive) {
        log.info("Updating webhook ID: {}", webhookId);
        
        Webhook webhook = getWebhookById(webhookId);
        
        if (name != null) webhook.setName(name);
        if (url != null) webhook.setUrl(url);
        if (events != null) webhook.setEvents(events);
        if (isActive != null) webhook.setIsActive(isActive);
        
        Webhook updated = webhookRepository.save(webhook);
        
        log.info("Webhook updated successfully");
        
        return updated;
    }
    
    /**
     * Delete webhook
     */
    @Transactional
    public void deleteWebhook(Long webhookId) {
        log.info("Deleting webhook ID: {}", webhookId);
        
        Webhook webhook = getWebhookById(webhookId);
        webhookRepository.delete(webhook);
        
        log.info("Webhook deleted successfully");
    }
    
    /**
     * Trigger webhook for an event
     */
    @Transactional
    public void triggerWebhook(Long tenantId, String eventType, Object payload) {
        log.info("Triggering webhooks for tenant {} with event: {}", tenantId, eventType);
        
        List<Webhook> webhooks = getActiveWebhooks(tenantId);
        
        for (Webhook webhook : webhooks) {
            if (webhook.supportsEvent(eventType)) {
                sendWebhookAsync(webhook, eventType, payload);
            }
        }
    }
    
    /**
     * Send webhook notification asynchronously
     */
    private void sendWebhookAsync(Webhook webhook, String eventType, Object payload) {
        new Thread(() -> {
            try {
                sendWebhook(webhook, eventType, payload);
            } catch (Exception e) {
                log.error("Failed to send webhook: {}", e.getMessage());
            }
        }).start();
    }
    
    /**
     * Send webhook HTTP request
     */
    @Transactional
    public void sendWebhook(Webhook webhook, String eventType, Object payload) {
        log.info("Sending webhook to URL: {}", webhook.getUrl());
        
        try {
            // Prepare payload
            WebhookPayload webhookPayload = new WebhookPayload(
                UUID.randomUUID().toString(),
                eventType,
                LocalDateTime.now(),
                webhook.getTenantId(),
                payload
            );
            
            // Generate signature
            String signature = generateSignature(webhookPayload.toString(), webhook.getSecretKey());
            
            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SIGNATURE_HEADER, signature);
            headers.set("X-Webhook-Event", eventType);
            headers.set("X-Webhook-ID", webhook.getId().toString());
            
            HttpEntity<WebhookPayload> request = new HttpEntity<>(webhookPayload, headers);
            
            // Send request with timeout
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );
            
            // Check response
            if (response.getStatusCode().is2xxSuccessful()) {
                webhook.recordSuccess();
                webhookRepository.save(webhook);
                log.info("Webhook sent successfully to: {}", webhook.getUrl());
            } else {
                throw new RuntimeException("Webhook returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", webhook.getUrl(), e.getMessage());
            webhook.recordFailure();
            webhookRepository.save(webhook);
            
            // Retry logic
            if (webhook.getRetryCount() > 0) {
                retryWebhook(webhook, eventType, payload);
            }
        }
    }
    
    /**
     * Retry failed webhook with exponential backoff
     */
    private void retryWebhook(Webhook webhook, String eventType, Object payload) {
        Integer retriesLeft = webhook.getRetryCount();
        
        if (retriesLeft > 0) {
            new Thread(() -> {
                try {
                    // Exponential backoff: 2^retry seconds
                    int delay = (int) Math.pow(2, 3 - retriesLeft) * 1000;
                    Thread.sleep(delay);
                    
                    webhook.setRetryCount(retriesLeft - 1);
                    sendWebhook(webhook, eventType, payload);
                    
                } catch (InterruptedException e) {
                    log.error("Webhook retry interrupted: {}", e.getMessage());
                }
            }).start();
        }
    }
    
    /**
     * Generate secret key for webhook signature
     */
    private String generateSecretKey() {
        return "whsec_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate HMAC signature for webhook payload
     */
    private String generateSignature(String payload, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            hmac.init(secretKeySpec);
            
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            log.error("Failed to generate signature: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Verify webhook signature
     */
    public boolean verifySignature(String payload, String signature, String secretKey) {
        String expectedSignature = generateSignature(payload, secretKey);
        return expectedSignature.equals(signature);
    }
    
    /**
     * Test webhook by sending a ping
     */
    public boolean testWebhook(Long webhookId) {
        log.info("Testing webhook ID: {}", webhookId);
        
        Webhook webhook = getWebhookById(webhookId);
        
        try {
            sendWebhook(webhook, "webhook.test", new TestPayload("Webhook test successful"));
            return true;
        } catch (Exception e) {
            log.error("Webhook test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get webhook statistics
     */
    public WebhookStats getWebhookStats(Long webhookId) {
        Webhook webhook = getWebhookById(webhookId);
        
        return new WebhookStats(
            webhook.getId(),
            webhook.getName(),
            webhook.getSuccessCount(),
            webhook.getFailureCount(),
            webhook.getLastTriggeredAt(),
            webhook.getIsActive()
        );
    }
    
    // Inner classes for webhook payload and stats
    
    public static class WebhookPayload {
        private String id;
        private String event;
        private LocalDateTime timestamp;
        private Long tenantId;
        private Object data;
        
        public WebhookPayload(String id, String event, LocalDateTime timestamp, 
                            Long tenantId, Object data) {
            this.id = id;
            this.event = event;
            this.timestamp = timestamp;
            this.tenantId = tenantId;
            this.data = data;
        }
        
        // Getters
        public String getId() { return id; }
        public String getEvent() { return event; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Long getTenantId() { return tenantId; }
        public Object getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("{\"id\":\"%s\",\"event\":\"%s\",\"timestamp\":\"%s\",\"tenantId\":%d}", 
                id, event, timestamp, tenantId);
        }
    }
    
    public static class TestPayload {
        private String message;
        
        public TestPayload(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
    }
    
    public static class WebhookStats {
        private Long id;
        private String name;
        private Long successCount;
        private Long failureCount;
        private LocalDateTime lastTriggered;
        private Boolean isActive;
        
        public WebhookStats(Long id, String name, Long successCount, 
                          Long failureCount, LocalDateTime lastTriggered, 
                          Boolean isActive) {
            this.id = id;
            this.name = name;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.lastTriggered = lastTriggered;
            this.isActive = isActive;
        }
        
        // Getters
        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getSuccessCount() { return successCount; }
        public Long getFailureCount() { return failureCount; }
        public LocalDateTime getLastTriggered() { return lastTriggered; }
        public Boolean getIsActive() { return isActive; }
        public Double getSuccessRate() {
            long total = successCount + failureCount;
            return total > 0 ? (successCount * 100.0) / total : 0.0;
        }
    }
}