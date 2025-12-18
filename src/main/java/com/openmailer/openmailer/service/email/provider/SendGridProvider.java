package com.openmailer.openmailer.service.email.provider;

import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.ProviderType;
import com.openmailer.openmailer.service.email.EmailSender;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * SendGrid email provider implementation
 */
public class SendGridProvider implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SendGridProvider.class);

    private final EmailProvider provider;
    private final SendGrid sendGrid;

    public SendGridProvider(EmailProvider provider) {
        this.provider = provider;

        // Get API key from configuration
        String apiKey = provider.getConfigurationMap().get("apiKey");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("SendGrid requires apiKey in configuration");
        }

        this.sendGrid = new SendGrid(apiKey);
        log.info("SendGrid Provider initialized");
    }

    @Override
    public EmailSendResponse send(EmailSendRequest emailRequest) throws EmailSendException {
        try {
            // Build sender email
            Email from = new Email(emailRequest.getFrom());
            if (emailRequest.getFromName() != null) {
                from.setName(emailRequest.getFromName());
            }

            // Build recipient email
            Email to = new Email(emailRequest.getTo());

            // Build subject
            String subject = emailRequest.getSubject();

            // Build mail object
            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(subject);

            // Add content
            if (emailRequest.getHtmlBody() != null) {
                Content htmlContent = new Content("text/html", emailRequest.getHtmlBody());
                mail.addContent(htmlContent);
            }

            if (emailRequest.getTextBody() != null) {
                Content textContent = new Content("text/plain", emailRequest.getTextBody());
                mail.addContent(textContent);
            }

            // Add personalization (to, cc, bcc)
            Personalization personalization = new Personalization();
            personalization.addTo(to);

            // Add CC if specified
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                for (String ccEmail : emailRequest.getCc()) {
                    personalization.addCc(new Email(ccEmail));
                }
            }

            // Add BCC if specified
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                for (String bccEmail : emailRequest.getBcc()) {
                    personalization.addBcc(new Email(bccEmail));
                }
            }

            mail.addPersonalization(personalization);

            // Add reply-to if specified
            if (emailRequest.getReplyTo() != null) {
                mail.setReplyTo(new Email(emailRequest.getReplyTo()));
            }

            // Add custom headers if specified
            if (emailRequest.getHeaders() != null && !emailRequest.getHeaders().isEmpty()) {
                for (var entry : emailRequest.getHeaders().entrySet()) {
                    mail.addHeader(entry.getKey(), entry.getValue());
                }
            }

            // Send email
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                // SendGrid returns message ID in X-Message-Id header
                String messageId = response.getHeaders().getOrDefault("X-Message-Id", "unknown");
                log.info("Email sent via SendGrid to: {}, MessageId: {}", emailRequest.getTo(), messageId);
                return new EmailSendResponse(true, messageId);
            } else {
                String errorMessage = "SendGrid returned status code: " + response.getStatusCode() + ", body: " + response.getBody();
                log.error("SendGrid send failed: {}", errorMessage);
                throw new EmailSendException(errorMessage);
            }

        } catch (IOException e) {
            log.error("SendGrid send failed with IOException", e);
            throw new EmailSendException("Failed to send email via SendGrid: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error sending email via SendGrid", e);
            throw new EmailSendException("Unexpected error sending email via SendGrid", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return provider != null
                && provider.getConfigurationMap() != null
                && provider.getConfigurationMap().containsKey("apiKey");
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.SENDGRID;
    }
}
