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
 * PasswordResetService - FIXED VERSION
 * Added comprehensive logging and better error handling
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
        
        log.info("═══════════════════════════════════════");
        log.info("🔐 PasswordResetService Initialized");
        log.info("  Token Expiry: {} minutes", tokenExpiryMinutes);
        log.info("═══════════════════════════════════════");
    }
    
    /**
     * Request password reset - Send email with reset link
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("═══════════════════════════════════════");
        log.info("🔐 PASSWORD RESET REQUEST");
        log.info("  Email: {}", email);
        log.info("═══════════════════════════════════════");
        
        try {
            // Step 1: Find user
            log.info("Step 1: Looking up user...");
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with email: {}", email);
                    return new IllegalArgumentException("User not found with email: " + email);
                });
            log.info("✅ User found: {} (ID: {})", user.getEmail(), user.getId());
            
            // Step 2: Invalidate existing tokens
            log.info("Step 2: Invalidating existing tokens...");
            var existingTokens = tokenRepository.findByUserAndUsedFalse(user);
            log.info("  Found {} existing tokens", existingTokens.size());
            existingTokens.forEach(token -> {
                token.setUsed(true);
                tokenRepository.save(token);
            });
            log.info("✅ Existing tokens invalidated");
            
            // Step 3: Create new reset token
            log.info("Step 3: Creating new reset token...");
            PasswordResetToken resetToken = new PasswordResetToken(user, tokenExpiryMinutes);
            PasswordResetToken savedToken = tokenRepository.save(resetToken);
            log.info("✅ Token created: {}...", savedToken.getToken().substring(0, 10));
            log.info("  Expires at: {}", savedToken.getExpiresAt());
            
            // Step 4: Send email
            log.info("Step 4: Sending password reset email...");
            try {
                emailService.sendPasswordResetEmail(user, savedToken.getToken());
                log.info("✅ Password reset email sent successfully");
            } catch (Exception emailError) {
                log.error("❌ Failed to send email: {}", emailError.getMessage());
                throw emailError; // Re-throw to make it visible
            }
            
            log.info("═══════════════════════════════════════");
            log.info("✅ PASSWORD RESET REQUEST COMPLETED");
            log.info("  Email: {}", email);
            log.info("═══════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ PASSWORD RESET REQUEST FAILED");
            log.error("  Email: {}", email);
            log.error("  Error: {}", e.getMessage());
            log.error("  Stack trace: ", e);
            log.error("═══════════════════════════════════════");
            throw e; // Re-throw to controller
        }
    }
    
    /**
     * Validate reset token
     */
    public boolean validateResetToken(String token) {
        log.info("Validating reset token: {}...", token.substring(0, Math.min(10, token.length())));
        
        boolean isValid = tokenRepository.findByToken(token)
            .map(resetToken -> {
                boolean valid = resetToken.isValid();
                log.info("Token validation result: {}", valid);
                if (!valid) {
                    log.warn("Token is invalid - Used: {}, Expired: {}", 
                        resetToken.getUsed(), resetToken.isExpired());
                }
                return valid;
            })
            .orElse(false);
        
        if (!isValid) {
            log.warn("Token not found or invalid");
        }
        
        return isValid;
    }
    
    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("═══════════════════════════════════════");
        log.info("🔐 RESETTING PASSWORD");
        log.info("  Token: {}...", token.substring(0, Math.min(10, token.length())));
        log.info("═══════════════════════════════════════");
        
        try {
            // Find and validate token
            log.info("Step 1: Finding reset token...");
            PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("❌ Invalid reset token");
                    return new IllegalArgumentException("Invalid reset token");
                });
            log.info("✅ Token found");
            
            log.info("Step 2: Validating token...");
            if (!resetToken.isValid()) {
                log.error("❌ Token is expired or already used");
                log.error("  Used: {}, Expired: {}", resetToken.getUsed(), resetToken.isExpired());
                throw new IllegalArgumentException("Reset token has expired or been used");
            }
            log.info("✅ Token is valid");
            
            // Get user and update password
            log.info("Step 3: Updating user password...");
            User user = resetToken.getUser();
            log.info("  User: {}", user.getEmail());
            
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("✅ Password updated");
            
            // Mark token as used
            log.info("Step 4: Marking token as used...");
            resetToken.markAsUsed();
            tokenRepository.save(resetToken);
            log.info("✅ Token marked as used");
            
            // Send confirmation email
            log.info("Step 5: Sending confirmation email...");
            try {
                emailService.sendPasswordChangedEmail(user);
                log.info("✅ Confirmation email sent");
            } catch (Exception e) {
                log.warn("Failed to send confirmation email (non-critical): {}", e.getMessage());
            }
            
            log.info("═══════════════════════════════════════");
            log.info("✅ PASSWORD RESET SUCCESSFUL");
            log.info("  User: {}", user.getEmail());
            log.info("═══════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ PASSWORD RESET FAILED");
            log.error("  Error: {}", e.getMessage());
            log.error("═══════════════════════════════════════");
            throw e;
        }
    }
    
    /**
     * Get user from reset token (for display purposes)
     */
    public User getUserFromResetToken(String token) {
        log.info("Getting user from reset token...");
        
        return tokenRepository.findByToken(token)
            .filter(resetToken -> {
                boolean valid = resetToken.isValid();
                if (!valid) {
                    log.warn("Token is invalid for user retrieval");
                }
                return valid;
            })
            .map(PasswordResetToken::getUser)
            .orElseThrow(() -> {
                log.error("Invalid or expired reset token");
                return new IllegalArgumentException("Invalid or expired reset token");
            });
    }
    
    /**
     * Clean up expired tokens (can be called by scheduled job)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired password reset tokens...");
        tokenRepository.deleteByExpiresAtBefore(java.time.LocalDateTime.now());
        log.info("Expired tokens cleaned up");
    }
}