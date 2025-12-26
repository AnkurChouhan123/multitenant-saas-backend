package com.saas.platform.repository;

import com.saas.platform.model.PasswordResetToken;
import com.saas.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//
// PasswordResetTokenRepository - Database operations for password reset tokens
 
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    //
// Find token by token string
     
    Optional<PasswordResetToken> findByToken(String token);
    
    //
// Find all unused tokens for a user
     
    List<PasswordResetToken> findByUserAndUsedFalse(User user);
    
    //
// Find all tokens for a user
     
    List<PasswordResetToken> findByUser(User user);
    
    //
// Delete expired tokens
     
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    //
// Find valid (unused and not expired) token
     
    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(
        String token, LocalDateTime now);
}