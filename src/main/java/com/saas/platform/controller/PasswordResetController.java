package com.saas.platform.controller;

import com.saas.platform.dto.PasswordResetRequest;
import com.saas.platform.dto.ResetPasswordRequest;
import com.saas.platform.model.User;
import com.saas.platform.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * PasswordResetController - REST API for password reset
 */
@RestController
@RequestMapping("/api/password-reset")
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }
    
    /**
     * POST /api/password-reset/request - Request password reset
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @RequestBody PasswordResetRequest request) {
        
        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "If an account exists with this email, you will receive a password reset link.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Don't reveal if email exists or not (security)
            Map<String, String> response = new HashMap<>();
            response.put("message", "If an account exists with this email, you will receive a password reset link.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * GET /api/password-reset/validate/{token} - Validate reset token
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<Map<String, Object>> validateToken(@PathVariable String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = passwordResetService.validateResetToken(token);
            
            if (isValid) {
                User user = passwordResetService.getUserFromResetToken(token);
                response.put("valid", true);
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName());
            } else {
                response.put("valid", false);
                response.put("message", "Token is invalid or has expired");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * POST /api/password-reset/reset - Reset password with token
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                response.put("message", "Passwords do not match");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate password strength
            if (request.getNewPassword().length() < 6) {
                response.put("message", "Password must be at least 6 characters");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Reset password
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            response.put("message", "Password reset successfully! You can now login with your new password.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }
}