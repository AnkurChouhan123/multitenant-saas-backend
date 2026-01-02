package com.saas.platform.service;

import com.saas.platform.model.TwoFactorAuth;
import com.saas.platform.model.User;
import com.saas.platform.repository.TwoFactorAuthRepository;
import com.saas.platform.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


 // TwoFactorAuthService - Handles all 2FA operations
 // Supports TOTP (Google Authenticator), Email OTP, and Backup Codes

@Service
public class TwoFactorAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthService.class);
    private static final String ISSUER = "SaaS Platform";
    private static final int BACKUP_CODES_COUNT = 10;
    
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;
    private final CodeVerifier verifier;
    
    public TwoFactorAuthService(TwoFactorAuthRepository twoFactorAuthRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.twoFactorAuthRepository = twoFactorAuthRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = new SystemTimeProvider();
        this.verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);
    }
    
    
     // Initialize 2FA for a user - Generate secret and QR code
     
    @Transactional
    public Map<String, Object> initializeTwoFactor(Long userId, String method) {
        log.info("Initializing 2FA for user ID: {} with method: {}", userId, method);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if 2FA already exists
        Optional<TwoFactorAuth> existing = twoFactorAuthRepository.findByUser(user);
        if (existing.isPresent() && existing.get().getIsEnabled()) {
            throw new IllegalStateException("2FA is already enabled for this user");
        }
        
        // Generate secret key
        String secret = new DefaultSecretGenerator().generate();
        
        // Create or update 2FA record
        TwoFactorAuth twoFactorAuth = existing.orElse(new TwoFactorAuth(user, secret));
        twoFactorAuth.setSecretKey(secret);
        twoFactorAuth.setMethod(method);
        twoFactorAuth.setIsEnabled(false);
        twoFactorAuth.setIsVerified(false);
        
        if ("EMAIL".equals(method)) {
            twoFactorAuth.setEmail(user.getEmail());
        }
        
        twoFactorAuth = twoFactorAuthRepository.save(twoFactorAuth);
        
        Map<String, Object> result = new HashMap<>();
        result.put("secret", secret);
        result.put("method", method);
        
        // Generate QR code for TOTP
        if ("TOTP".equals(method)) {
            try {
                QrData data = new QrData.Builder()
                        .label(user.getEmail())
                        .secret(secret)
                        .issuer(ISSUER)
                        .algorithm(HashingAlgorithm.SHA1)
                        .digits(6)
                        .period(30)
                        .build();
                
                QrGenerator generator = new ZxingPngQrGenerator();
                byte[] imageData = generator.generate(data);
                String qrCodeBase64 = Base64.getEncoder().encodeToString(imageData);
                
                result.put("qrCode", "data:image/png;base64," + qrCodeBase64);
                result.put("manualEntryKey", secret);
            } catch (QrGenerationException e) {
                log.error("Failed to generate QR code", e);
                throw new RuntimeException("Failed to generate QR code", e);
            }
        }
        
        log.info("2FA initialized successfully for user: {}", user.getEmail());
        return result;
    }
    
    
    //  Verify and enable 2FA
     
    @Transactional
    public Map<String, Object> verifyAndEnableTwoFactor(Long userId, String code) {
        log.info("Verifying 2FA code for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("2FA not initialized"));
        
        // Verify the code
        boolean isValid = verifyCode(twoFactorAuth.getSecretKey(), code);
        
        if (!isValid) {
            log.warn("Invalid 2FA code for user: {}", user.getEmail());
            throw new IllegalArgumentException("Invalid verification code");
        }
        
        // Enable 2FA
        twoFactorAuth.setIsEnabled(true);
        twoFactorAuth.setIsVerified(true);
        
        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();
        String encryptedBackupCodes = backupCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(","));
        
        twoFactorAuth.setBackupCodes(encryptedBackupCodes);
        twoFactorAuthRepository.save(twoFactorAuth);
        
        log.info("2FA enabled successfully for user: {}", user.getEmail());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("backupCodes", backupCodes);
        result.put("message", "2FA enabled successfully. Save these backup codes in a safe place!");
        
        return result;
    }
    
    
     // Verify 2FA code during login
     
    public boolean verifyLoginCode(User user, String code) {
        log.info("Verifying 2FA login code for user: {}", user.getEmail());
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("2FA not enabled"));
        
        if (!twoFactorAuth.getIsEnabled()) {
            throw new IllegalStateException("2FA is not enabled for this user");
        }
        
        if (twoFactorAuth.isLocked()) {
            log.warn("2FA is locked for user: {}", user.getEmail());
            throw new IllegalStateException("Too many failed attempts. Please try again later.");
        }
        
        // Check if it's a backup code
        if (code.length() == 8 && code.matches("[A-Z0-9]+")) {
            return verifyBackupCode(twoFactorAuth, code);
        }
        
        // Verify TOTP code
        boolean isValid = verifyCode(twoFactorAuth.getSecretKey(), code);
        
        if (isValid) {
            twoFactorAuth.recordSuccessfulUse();
            twoFactorAuthRepository.save(twoFactorAuth);
            log.info("2FA verification successful for user: {}", user.getEmail());
        } else {
            twoFactorAuth.incrementFailedAttempts();
            twoFactorAuthRepository.save(twoFactorAuth);
            log.warn("2FA verification failed for user: {}", user.getEmail());
        }
        
        return isValid;
    }
    
    
     // Verify TOTP code
     
    private boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
    
    
     // Verify backup code
     
    private boolean verifyBackupCode(TwoFactorAuth twoFactorAuth, String code) {
        String backupCodesStr = twoFactorAuth.getBackupCodes();
        if (backupCodesStr == null || backupCodesStr.isEmpty()) {
            return false;
        }
        
        List<String> encryptedCodes = new ArrayList<>(Arrays.asList(backupCodesStr.split(",")));
        
        for (int i = 0; i < encryptedCodes.size(); i++) {
            if (passwordEncoder.matches(code, encryptedCodes.get(i))) {
                // Remove used backup code
                encryptedCodes.remove(i);
                twoFactorAuth.setBackupCodes(String.join(",", encryptedCodes));
                twoFactorAuth.recordSuccessfulUse();
                twoFactorAuthRepository.save(twoFactorAuth);
                
                log.info("Backup code used successfully. Remaining codes: {}", encryptedCodes.size());
                return true;
            }
        }
        
        return false;
    }
    
    
     // Generate backup codes
     
    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append(chars.charAt(random.nextInt(chars.length())));
            }
            codes.add(code.toString());
        }
        
        return codes;
    }
    
    
    //  Disable 2FA for a user
     
    @Transactional
    public void disableTwoFactor(Long userId) {
        log.info("Disabling 2FA for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("2FA not found"));
        
        twoFactorAuth.setIsEnabled(false);
        twoFactorAuth.setIsVerified(false);
        twoFactorAuthRepository.save(twoFactorAuth);
        
        log.info("2FA disabled successfully for user: {}", user.getEmail());
    }
    
    
     // Check if user has 2FA enabled
     
    public boolean isTwoFactorEnabled(User user) {
        return twoFactorAuthRepository.findByUser(user)
                .map(TwoFactorAuth::getIsEnabled)
                .orElse(false);
    }
    
    
//      Get 2FA status for a user
     
    public Map<String, Object> getTwoFactorStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository.findByUser(user);
        
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", twoFactorAuth.map(TwoFactorAuth::getIsEnabled).orElse(false));
        status.put("verified", twoFactorAuth.map(TwoFactorAuth::getIsVerified).orElse(false));
        status.put("method", twoFactorAuth.map(TwoFactorAuth::getMethod).orElse(null));
        
        return status;
    }
    
    
     // Send email OTP
     
    public void sendEmailOTP(User user) {
        log.info("Sending email OTP to: {}", user.getEmail());
        
        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));
        
        // Store OTP temporarily (you might want to create a separate table for this)
        // For now, we'll just send it
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .otp-box { background: #f3f4f6; padding: 20px; text-align: center; 
                               font-size: 32px; font-weight: bold; letter-spacing: 5px;
                               border-radius: 8px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your Login Code</h2>
                    <p>Hi %s,</p>
                    <p>Use this code to complete your login:</p>
                    <div class="otp-box">%s</div>
                    <p><strong>This code expires in 5 minutes.</strong></p>
                    <p>If you didn't request this code, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(user.getFirstName(), otp);
        
        emailService.sendEmail(user.getEmail(), "Your Login Code", htmlContent);
    }
    
    
//     Regenerate backup codes
     
    @Transactional
    public List<String> regenerateBackupCodes(Long userId) {
        log.info("Regenerating backup codes for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("2FA not enabled"));
        
        List<String> backupCodes = generateBackupCodes();
        String encryptedBackupCodes = backupCodes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.joining(","));
        
        twoFactorAuth.setBackupCodes(encryptedBackupCodes);
        twoFactorAuthRepository.save(twoFactorAuth);
        
        log.info("Backup codes regenerated successfully");
        return backupCodes;
    }
}