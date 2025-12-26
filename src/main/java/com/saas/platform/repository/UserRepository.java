package com.saas.platform.repository;

import com.saas.platform.model.User;
import com.saas.platform.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//
// UserRepository - Database operations for User
 
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    //
// Find user by email (for login)
     
    Optional<User> findByEmail(String email);
    
    //
// Check if email exists
     
    boolean existsByEmail(String email);
    
    //
// Find all users belonging to a specific tenant
     
    List<User> findByTenantId(Long tenantId);
    
    //
// Find user by email and tenant (ensures tenant isolation)
     
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);
    
    //
// Count users in a tenant
     
    long countByTenantId(Long tenantId);
    
    boolean existsByRole(UserRole role);

}