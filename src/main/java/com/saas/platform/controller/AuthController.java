package com.saas.platform.controller;

import com.saas.platform.dto.*;
import com.saas.platform.security.JwtUtil;
import com.saas.platform.service.AuthService;
import com.saas.platform.service.TwoFactorAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    
    private final AuthService authService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final JwtUtil jwtUtil;
    
    public AuthController(AuthService authService, 
                         TwoFactorAuthService twoFactorAuthService,
                         JwtUtil jwtUtil) {
        this.authService = authService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    
    
    
     // Initialize 2FA setup - Generate QR code
     
    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorResponse> setupTwoFactor(
            @RequestBody TwoFactorSetupRequest request,
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        Map<String, Object> result = twoFactorAuthService.initializeTwoFactor(userId, request.getMethod());
        
        TwoFactorResponse response = new TwoFactorResponse();
        response.setSuccess(true);
        response.setSecret((String) result.get("secret"));
        response.setQrCode((String) result.get("qrCode"));
        response.setManualEntryKey((String) result.get("manualEntryKey"));
        response.setMethod((String) result.get("method"));
        response.setMessage("Scan the QR code with your authenticator app");
        
        return ResponseEntity.ok(response);
    }
    
    
    //  Verify 2FA code and enable 2FA
     
    @PostMapping("/2fa/verify")
    public ResponseEntity<TwoFactorResponse> verifyTwoFactor(
            @RequestBody TwoFactorVerifyRequest request,
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        Map<String, Object> result = twoFactorAuthService.verifyAndEnableTwoFactor(userId, request.getCode());
        
        TwoFactorResponse response = new TwoFactorResponse();
        response.setSuccess((Boolean) result.get("success"));
        response.setMessage((String) result.get("message"));
        response.setBackupCodes((List<String>) result.get("backupCodes"));
        
        return ResponseEntity.ok(response);
    }
    
    
    //  Get 2FA status
     
    @GetMapping("/2fa/status")
    public ResponseEntity<TwoFactorResponse> getTwoFactorStatus(
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        Map<String, Object> status = twoFactorAuthService.getTwoFactorStatus(userId);
        
        TwoFactorResponse response = new TwoFactorResponse();
        response.setEnabled((Boolean) status.get("enabled"));
        response.setVerified((Boolean) status.get("verified"));
        response.setMethod((String) status.get("method"));
        
        return ResponseEntity.ok(response);
    }
    
    
    //  Disable 2FA
     
    @PostMapping("/2fa/disable")
    public ResponseEntity<TwoFactorResponse> disableTwoFactor(
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        twoFactorAuthService.disableTwoFactor(userId);
        
        TwoFactorResponse response = new TwoFactorResponse();
        response.setSuccess(true);
        response.setMessage("Two-factor authentication disabled successfully");
        
        return ResponseEntity.ok(response);
    }
    
    
     // Regenerate backup codes
     
    @PostMapping("/2fa/backup-codes/regenerate")
    public ResponseEntity<TwoFactorResponse> regenerateBackupCodes(
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        List<String> backupCodes = twoFactorAuthService.regenerateBackupCodes(userId);
        
        TwoFactorResponse response = new TwoFactorResponse();
        response.setSuccess(true);
        response.setBackupCodes(backupCodes);
        response.setMessage("New backup codes generated. Save them in a safe place!");
        
        return ResponseEntity.ok(response);
    }
    
    // HELPER METHODS
    
    private Long extractUserIdFromToken(String bearerToken) {
        String token = bearerToken.substring(7); // Remove "Bearer " prefix
        return jwtUtil.extractUserId(token);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}