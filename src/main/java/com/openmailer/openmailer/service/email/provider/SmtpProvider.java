package com.openmailer.openmailer.service.email.provider;

import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.service.email.EmailSender;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

/**
 * SMTP email provider implementation using JavaMail
 */
public class SmtpProvider implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpProvider.class);

    private final EmailProvider provider;
    private final Session session;
    private final String username;
    private final String password;

    public SmtpProvider(EmailProvider provider) {
        this.provider = provider;

        // Parse configuration
        String host = provider.getConfiguration().get("host");
        String portStr = provider.getConfiguration().getOrDefault("port", "587");
        this.username = provider.getConfiguration().get("username");
        this.password = provider.getConfiguration().get("password");
        String encryption = provider.getConfiguration().getOrDefault("encryption", "TLS"); // TLS or SSL

        if (host == null || username == null || password == null) {
            throw new IllegalArgumentException("SMTP requires host, username, and password");
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = 587; // Default to TLS port
        }

        // Configure SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");

        // Configure encryption
        if ("SSL".equalsIgnoreCase(encryption)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", port);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            // Default to TLS
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Additional properties for better compatibility
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        // Create session with authentication
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        log.info("SMTP Provider initialized for host: {}:{} with {} encryption", host, port, encryption);
    }

    @Override
    public EmailSendResponse send(EmailSendRequest emailRequest) throws EmailSendException {
        try {
            // Create message
            MimeMessage message = new MimeMessage(session);

            // Set from address
            if (emailRequest.getFromName() != null) {
                message.setFrom(new InternetAddress(emailRequest.getFrom(), emailRequest.getFromName()));
            } else {
                message.setFrom(new InternetAddress(emailRequest.getFrom()));
            }

            // Set to address
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailRequest.getTo()));

            // Set CC if specified
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(String.join(",", emailRequest.getCc())));
            }

            // Set BCC if specified
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC,
                        InternetAddress.parse(String.join(",", emailRequest.getBcc())));
            }

            // Set reply-to if specified
            if (emailRequest.getReplyTo() != null) {
                message.setReplyTo(InternetAddress.parse(emailRequest.getReplyTo()));
            }

            // Set subject
            message.setSubject(emailRequest.getSubject(), "UTF-8");

            // Build multipart content if we have both HTML and text
            if (emailRequest.getHtmlBody() != null && emailRequest.getTextBody() != null) {
                MimeMultipart multipart = new MimeMultipart("alternative");

                // Add text part
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(emailRequest.getTextBody(), "UTF-8");
                multipart.addBodyPart(textPart);

                // Add HTML part
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(emailRequest.getHtmlBody(), "text/html; charset=UTF-8");
                multipart.addBodyPart(htmlPart);

                message.setContent(multipart);

            } else if (emailRequest.getHtmlBody() != null) {
                // HTML only
                message.setContent(emailRequest.getHtmlBody(), "text/html; charset=UTF-8");

            } else if (emailRequest.getTextBody() != null) {
                // Text only
                message.setText(emailRequest.getTextBody(), "UTF-8");

            } else {
                throw new EmailSendException("Email must have either HTML or text body");
            }

            // Add custom headers if specified
            if (emailRequest.getHeaders() != null && !emailRequest.getHeaders().isEmpty()) {
                for (var entry : emailRequest.getHeaders().entrySet()) {
                    message.addHeader(entry.getKey(), entry.getValue());
                }
            }

            // Generate message ID
            String messageId = UUID.randomUUID().toString() + "@" + provider.getName();
            message.setHeader("Message-ID", "<" + messageId + ">");

            // Send message
            Transport.send(message);

            log.info("Email sent via SMTP to: {}, MessageId: {}", emailRequest.getTo(), messageId);

            return new EmailSendResponse(true, messageId);

        } catch (MessagingException e) {
            log.error("SMTP send failed: {}", e.getMessage(), e);
            throw new EmailSendException("Failed to send email via SMTP: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error sending email via SMTP", e);
            throw new EmailSendException("Unexpected error sending email via SMTP", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return provider != null
                && provider.getConfiguration() != null
                && provider.getConfiguration().containsKey("host")
                && provider.getConfiguration().containsKey("username")
                && provider.getConfiguration().containsKey("password");
    }

    @Override
    public EmailProvider.ProviderType getProviderType() {
        return EmailProvider.ProviderType.SMTP;
    }
}
