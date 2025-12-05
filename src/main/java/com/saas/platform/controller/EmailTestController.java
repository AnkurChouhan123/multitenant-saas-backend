package com.saas.platform.controller;

import com.saas.platform.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * EmailTestController - For testing email configuration
 * REMOVE THIS IN PRODUCTION!
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:3000")
public class EmailTestController {
    
    private final EmailService emailService;
    
    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Test email sending
     * GET /api/test/email?to=your@email.com
     */
    @GetMapping("/email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam String to) {
        Map<String, String> response = new HashMap<>();
        
        try {
            emailService.sendTestEmail(to);
            response.put("status", "success");
            response.put("message", "Test email sent to " + to);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
    }
}