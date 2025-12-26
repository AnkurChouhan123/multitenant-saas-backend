package com.saas.platform.repository;

import com.saas.platform.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

//
// TenantRepository - Database operations for Tenant
// JpaRepository provides built-in methods:
// - save(), findAll(), findById(), delete(), etc.
 
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    //
// Find tenant by subdomain
// Spring automatically implements this based on method name!
     
    Optional<Tenant> findBySubdomain(String subdomain);
    
    //
// Check if subdomain already exists
     
    boolean existsBySubdomain(String subdomain);
    
    //
// Find tenant by name
     
    Optional<Tenant> findByName(String name);
}