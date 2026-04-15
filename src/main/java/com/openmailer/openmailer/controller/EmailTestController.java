package com.openmailer.openmailer.controller;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for sending test emails via configured SMTP provider
 */
@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    private static final Logger log = LoggerFactory.getLogger(EmailTestController.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String mailHost;
    private final String quickTestRecipient;

    public EmailTestController(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailFrom,
            @Value("${spring.mail.host:unknown}") String mailHost,
            @Value("${app.test-email.recipient:}") String quickTestRecipient) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.mailHost = mailHost;
        this.quickTestRecipient = quickTestRecipient;
    }

    /**
     * Send a test email
     *
     * @param toEmail Recipient email address
     * @param subject Email subject (optional)
     * @return Response with send status
     */
    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestParam String toEmail,
            @RequestParam(required = false, defaultValue = "OpenMailer Test Email") String subject) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (mailFrom == null || mailFrom.isBlank()) {
                response.put("success", false);
                response.put("error", "MAIL_USERNAME or spring.mail.username must be configured before sending test emails.");
                return ResponseEntity.badRequest().body(response);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email properties
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setFrom(mailFrom, "OpenMailer Test");

            // Create HTML body
            String htmlContent = createTestEmailHtml(toEmail);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            log.info("Test email sent successfully to: {}", toEmail);

            response.put("success", true);
            response.put("message", "Test email sent successfully!");
            response.put("to", toEmail);
            response.put("sentAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(response);

        } catch (MessagingException e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("error", "Failed to send email: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            log.error("Unexpected error sending test email", e);

            response.put("success", false);
            response.put("error", "Unexpected error: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Simple GET endpoint to send test email with default recipient
     */
    @GetMapping("/send-quick-test")
    public ResponseEntity<Map<String, Object>> sendQuickTest() {
        if (quickTestRecipient == null || quickTestRecipient.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Configure app.test-email.recipient to use the quick test endpoint.");
            return ResponseEntity.badRequest().body(response);
        }

        return sendTestEmail(quickTestRecipient, "Quick Test from OpenMailer");
    }

    /**
     * Health check for email configuration
     */
    @GetMapping("/email-config-check")
    public ResponseEntity<Map<String, Object>> checkEmailConfig() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Try to create a message to verify configuration
            mailSender.createMimeMessage();

            response.put("configured", true);
            response.put("message", "Email configuration is valid");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("configured", false);
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Create HTML content for test email
     */
    private String createTestEmailHtml(String recipient) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                              color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .info-box { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #667eea; }
                    .success { color: #10b981; font-weight: bold; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 OpenMailer Test</h1>
                        <p>Email Configuration Test Successful</p>
                    </div>
                    <div class="content">
                        <p class="success">✓ Your email configuration is working correctly!</p>

                        <div class="info-box">
                            <strong>Test Details:</strong><br>
                            📧 Recipient: %s<br>
                            🕐 Sent At: %s<br>
                            📮 Provider Host: %s<br>
                            ⚡ Status: Successfully Delivered
                        </div>

                        <p>This is a test email sent from your OpenMailer application.
                           If you received this email, it means your SMTP configuration is working properly.</p>

                        <p><strong>Next Steps:</strong></p>
                        <ul>
                            <li>✅ SMTP configuration verified</li>
                            <li>📧 Ready to send bulk emails</li>
                            <li>🎯 Start creating campaigns</li>
                        </ul>

                        <div class="footer">
                            <p>Powered by <strong>OpenMailer</strong></p>
                            <p>Open Source Bulk Email System</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(recipient, timestamp, mailHost);
    }
}
