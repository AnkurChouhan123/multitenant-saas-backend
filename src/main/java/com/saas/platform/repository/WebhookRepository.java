package com.saas.platform.repository;

import com.saas.platform.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

//
// WebhookRepository - Database operations for Webhooks
 
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    
    //
// Find all webhooks for a tenant
     
    List<Webhook> findByTenantId(Long tenantId);
    
    //
// Find active webhooks for a tenant
     
    List<Webhook> findByTenantIdAndIsActiveTrue(Long tenantId);
    
    //
// Find webhooks that support a specific event
     
    List<Webhook> findByTenantIdAndIsActiveTrueAndEventsContaining(Long tenantId, String event);
    
    //
// Count active webhooks for a tenant
     
    long countByTenantIdAndIsActiveTrue(Long tenantId);
    
    //
// Find webhooks that haven't been triggered recently
     
    List<Webhook> findByLastTriggeredAtBefore(LocalDateTime dateTime);
    
    //
// Find webhooks with high failure rates
     
    List<Webhook> findByFailureCountGreaterThan(Long threshold);
}