package com.saas.platform.repository;

import com.saas.platform.model.TwoFactorAuth;
import com.saas.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


// TwoFactorAuthRepository - Database operations for 2FA

@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {
    
    
// Find 2FA settings by user

    Optional<TwoFactorAuth> findByUser(User user);
    
    
// Find 2FA settings by user ID

    Optional<TwoFactorAuth> findByUserId(Long userId);
    
    
// Check if user has 2FA enabled

    boolean existsByUserAndIsEnabledTrue(User user);
    
  
// Delete 2FA settings by user

    void deleteByUser(User user);
}