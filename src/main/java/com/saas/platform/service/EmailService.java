package com.saas.platform.service;

import com.saas.platform.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

//
// EmailService - FIXED VERSION
// Fixed issues:
// 1. Better error handling and logging
// 2. Removed @Async from main method to ensure errors are visible
// 3. Added template existence checking
// 4. Improved exception handling
 
@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    
    @Value("${app.mail.from}")
    private String mailFrom;
    
    @Value("${app.mail.from-name}")
    private String mailFromName;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        
        log.info("═══════════════════════════════════════");
        log.info("📧 EmailService Initialized");
        log.info("  Mail From: {}", mailFrom);
        log.info("  Mail From Name: {}", mailFromName);
        log.info("  Frontend URL: {}", frontendUrl);
        log.info("═══════════════════════════════════════");
    }
    
    //
// Send email - SYNCHRONOUS to see errors immediately
     
    public void sendEmail(String to, String subject, String htmlContent) {
        log.info("═══════════════════════════════════════");
        log.info("📧 SENDING EMAIL");
        log.info("  To: {}", to);
        log.info("  Subject: {}", subject);
        log.info("  From: {} <{}>", mailFromName, mailFrom);
        log.info("═══════════════════════════════════════");
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, 
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );
            
            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            log.info("📤 Sending email via SMTP...");
            mailSender.send(message);
            
            log.info("═══════════════════════════════════════");
            log.info("✅ EMAIL SENT SUCCESSFULLY");
            log.info("  Recipient: {}", to);
            log.info("═══════════════════════════════════════");
            
        } catch (MessagingException e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ MESSAGING EXCEPTION");
            log.error("  Error: {}", e.getMessage());
            log.error("  Cause: {}", e.getCause() != null ? e.getCause().getMessage() : "None");
            log.error("═══════════════════════════════════════");
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ EMAIL SEND FAILED");
            log.error("  To: {}", to);
            log.error("  Error: {}", e.getMessage());
            log.error("  Stack trace: ", e);
            log.error("═══════════════════════════════════════");
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    //
// Send email asynchronously (for non-critical emails)
     
    @Async
    public void sendEmailAsync(String to, String subject, String htmlContent) {
        try {
            sendEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Async email failed: {}", e.getMessage());
        }
    }
    
    //
// Send welcome email to new user
     
    public void sendWelcomeEmail(User user, String tenantName) {
        log.info("Preparing welcome email for: {}", user.getEmail());
        
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("userEmail", user.getEmail());
            context.setVariable("tenantName", tenantName);
            context.setVariable("loginUrl", frontendUrl + "/login");
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            
            String htmlContent = templateEngine.process("welcome-email", context);
            
            sendEmail(
                user.getEmail(),
                "Welcome to " + tenantName + "! 🎉",
                htmlContent
            );
            
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage(), e);
            // Don't throw - welcome email is not critical
        }
    }
    
    //
// Send password reset email - CRITICAL, must work
     
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("═══════════════════════════════════════");
        log.info("🔐 PREPARING PASSWORD RESET EMAIL");
        log.info("  User: {}", user.getEmail());
        log.info("  Token: {}...", resetToken.substring(0, Math.min(10, resetToken.length())));
        log.info("═══════════════════════════════════════");
        
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            log.info("📍 Reset URL: {}", resetUrl);
            
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expiryMinutes", 30);
            
            log.info("📝 Processing email template...");
            String htmlContent;
            try {
                htmlContent = templateEngine.process("password-reset-email", context);
                log.info("✅ Template processed successfully");
            } catch (Exception e) {
                log.error("❌ Template processing failed, using fallback HTML");
                htmlContent = createFallbackPasswordResetEmail(user.getFirstName(), resetUrl);
            }
            
            log.info("📧 Sending password reset email...");
            sendEmail(
                user.getEmail(),
                "Reset Your Password - Action Required",
                htmlContent
            );
            
            log.info("═══════════════════════════════════════");
            log.info("✅ PASSWORD RESET EMAIL SENT");
            log.info("═══════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("═══════════════════════════════════════");
            log.error("❌ FAILED TO SEND PASSWORD RESET EMAIL");
            log.error("  User: {}", user.getEmail());
            log.error("  Error: {}", e.getMessage());
            log.error("  Stack trace: ", e);
            log.error("═══════════════════════════════════════");
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    //
// Fallback HTML email (no template needed)
     
    private String createFallbackPasswordResetEmail(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; background: #f9f9f9; }
                    .header { background: #ef4444; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background: white; }
                    .button { display: inline-block; padding: 12px 30px; background: #ef4444; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #fee2e2; padding: 15px; margin: 20px 0; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Reset Your Password</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>%s</strong>,</p>
                        <p>We received a request to reset your password. Click the button below:</p>
                        <center>
                            <a href="%s" class="button">Reset Password</a>
                        </center>
                        <div class="warning">
                            <strong>⏰ This link expires in 30 minutes.</strong>
                        </div>
                        <p>If you didn't request this, please ignore this email.</p>
                        <p>Or copy this link: <br><a href="%s">%s</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl, resetUrl);
    }
    
    //
// Send password changed confirmation email
     
    public void sendPasswordChangedEmail(User user) {
        log.info("Sending password changed confirmation to: {}", user.getEmail());
        
        try {
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #10b981; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9f9f9; }
                        .success { background: #d1fae5; padding: 15px; margin: 20px 0; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Password Changed Successfully ✅</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <div class="success">
                                <strong>✅ Your password has been changed successfully!</strong>
                            </div>
                            <p>You can now login with your new password.</p>
                            <p>If you didn't make this change, please contact support immediately.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(user.getFirstName());
            
            sendEmailAsync(
                user.getEmail(),
                "Your Password Was Changed",
                htmlContent
            );
            
        } catch (Exception e) {
            log.error("Failed to send password changed email: {}", e.getMessage());
            // Don't throw - this is just a notification
        }
    }
    
    //
// Test email configuration
     
    public void sendTestEmail(String toEmail) {
        log.info("Sending test email to: {}", toEmail);
        
        String htmlContent = """
            <html>
            <body>
                <h2>Test Email ✅</h2>
                <p>If you received this, your email configuration is working correctly!</p>
                <p>Timestamp: %s</p>
            </body>
            </html>
            """.formatted(java.time.LocalDateTime.now());
        
        sendEmail(toEmail, "Test Email from SaaS Platform", htmlContent);
    }
}