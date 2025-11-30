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

/**
 * EmailService - Handles all email sending operations
 */
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
    }
    
    /**
     * Send email asynchronously
     */
    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        log.info("Sending email to: {}", to);
        
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
            
            mailSender.send(message);
            
            log.info("✅ Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send welcome email to new user
     */
    public void sendWelcomeEmail(User user, String tenantName) {
        log.info("Sending welcome email to: {}", user.getEmail());
        
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
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Sending password reset email to: {}", user.getEmail());
        
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expiryMinutes", 30);
            
            String htmlContent = templateEngine.process("password-reset-email", context);
            
            sendEmail(
                user.getEmail(),
                "Reset Your Password",
                htmlContent
            );
            
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }
    
    /**
     * Send password changed confirmation email
     */
    public void sendPasswordChangedEmail(User user) {
        log.info("Sending password changed confirmation to: {}", user.getEmail());
        
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("supportUrl", frontendUrl + "/support");
            
            String htmlContent = templateEngine.process("password-changed-email", context);
            
            sendEmail(
                user.getEmail(),
                "Your Password Was Changed",
                htmlContent
            );
            
        } catch (Exception e) {
            log.error("Failed to send password changed email: {}", e.getMessage());
        }
    }
    
    /**
     * Send account verification email
     */
    public void sendVerificationEmail(User user, String verificationToken) {
        log.info("Sending verification email to: {}", user.getEmail());
        
        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("verificationUrl", verificationUrl);
            
            String htmlContent = templateEngine.process("verification-email", context);
            
            sendEmail(
                user.getEmail(),
                "Please Verify Your Email",
                htmlContent
            );
            
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
    }
    
    /**
     * Send generic notification email
     */
    public void sendNotificationEmail(User user, String subject, String message) {
        log.info("Sending notification email to: {}", user.getEmail());
        
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("message", message);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            
            String htmlContent = templateEngine.process("notification-email", context);
            
            sendEmail(user.getEmail(), subject, htmlContent);
            
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }
}