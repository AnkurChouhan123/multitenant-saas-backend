package com.saas.platform.service;

import com.saas.platform.model.PasswordResetToken;
import com.saas.platform.model.User;
import com.saas.platform.repository.PasswordResetTokenRepository;
import com.saas.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PasswordResetService - Handles password reset logic
 */
@Service
public class PasswordResetService {
    
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${app.password-reset.token-expiry:30}")
    private int tokenExpiryMinutes;
    
    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                               UserRepository userRepository,
                               EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    /**
     * Request password reset - Send email with reset link
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);
        
        // Find user by email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        
        // Invalidate any existing tokens for this user
        tokenRepository.findByUserAndUsedFalse(user)
            .forEach(token -> {
                token.setUsed(true);
                tokenRepository.save(token);
            });
        
        // Create new reset token
        PasswordResetToken resetToken = new PasswordResetToken(user, tokenExpiryMinutes);
        tokenRepository.save(resetToken);
        
        // Send reset email
        emailService.sendPasswordResetEmail(user, resetToken.getToken());
        
        log.info("✅ Password reset email sent to: {}", email);
    }
    
    /**
     * Validate reset token
     */
    public boolean validateResetToken(String token) {
        return tokenRepository.findByToken(token)
            .map(PasswordResetToken::isValid)
            .orElse(false);
    }
    
    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Attempting to reset password with token: {}", token);
        
        // Find and validate token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Reset token has expired or been used");
        }
        
        // Get user and update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);
        
        // Send confirmation email
        emailService.sendPasswordChangedEmail(user);
        
        log.info("✅ Password reset successfully for user: {}", user.getEmail());
    }
    
    /**
     * Get user from reset token (for display purposes)
     */
    public User getUserFromResetToken(String token) {
        return tokenRepository.findByToken(token)
            .filter(PasswordResetToken::isValid)
            .map(PasswordResetToken::getUser)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
    }
    
    /**
     * Clean up expired tokens (can be called by scheduled job)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(java.time.LocalDateTime.now());
        log.info("Cleaned up expired password reset tokens");
    }
}