package com.saas.platform.repository;

import com.saas.platform.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//
// SubscriptionRepository - Database operations for Subscription
 
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    //
// Find subscription by tenant ID
     
    Optional<Subscription> findByTenantId(Long tenantId);
    
    //
// Check if tenant has active subscription
     
    boolean existsByTenantIdAndIsActiveTrue(Long tenantId);
}