package com.openmailer.openmailer.service.email.provider;

import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.ProviderType;
import com.openmailer.openmailer.service.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * AWS SES email provider implementation
 */
public class AwsSesProvider implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(AwsSesProvider.class);

    private final EmailProvider provider;
    private final SesClient sesClient;

    public AwsSesProvider(EmailProvider provider) {
        this.provider = provider;

        // Parse configuration
        String accessKey = provider.getConfigurationMap().get("accessKey");
        String secretKey = provider.getConfigurationMap().get("secretKey");
        String regionStr = provider.getConfigurationMap().getOrDefault("region", "us-east-1");

        if (accessKey == null || secretKey == null) {
            throw new IllegalArgumentException("AWS SES requires accessKey and secretKey");
        }

        // Create credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        // Build SES client
        this.sesClient = SesClient.builder()
                .region(Region.of(regionStr))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("AWS SES Provider initialized for region: {}", regionStr);
    }

    @Override
    public EmailSendResponse send(EmailSendRequest emailRequest) throws EmailSendException {
        try {
            // Build email content
            Content subject = Content.builder()
                    .data(emailRequest.getSubject())
                    .build();

            Body.Builder bodyBuilder = Body.builder();

            if (emailRequest.getHtmlBody() != null) {
                bodyBuilder.html(Content.builder()
                        .data(emailRequest.getHtmlBody())
                        .build());
            }

            if (emailRequest.getTextBody() != null) {
                bodyBuilder.text(Content.builder()
                        .data(emailRequest.getTextBody())
                        .build());
            }

            Message message = Message.builder()
                    .subject(subject)
                    .body(bodyBuilder.build())
                    .build();

            // Build sender
            String from = emailRequest.getFromName() != null
                    ? emailRequest.getFromName() + " <" + emailRequest.getFrom() + ">"
                    : emailRequest.getFrom();

            // Build request
            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(from)
                    .destination(Destination.builder()
                            .toAddresses(emailRequest.getTo())
                            .build())
                    .message(message);

            // Add reply-to if specified
            if (emailRequest.getReplyTo() != null) {
                requestBuilder.replyToAddresses(emailRequest.getReplyTo());
            }

            // Add CC if specified
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                requestBuilder.destination(Destination.builder()
                        .toAddresses(emailRequest.getTo())
                        .ccAddresses(emailRequest.getCc())
                        .build());
            }

            // Add BCC if specified
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                requestBuilder.destination(Destination.builder()
                        .toAddresses(emailRequest.getTo())
                        .ccAddresses(emailRequest.getCc() != null ? emailRequest.getCc() : java.util.Collections.emptyList())
                        .bccAddresses(emailRequest.getBcc())
                        .build());
            }

            // Send email
            SendEmailResponse response = sesClient.sendEmail(requestBuilder.build());

            log.info("Email sent via AWS SES to: {}, MessageId: {}", emailRequest.getTo(), response.messageId());

            return new EmailSendResponse(true, response.messageId());

        } catch (SesException e) {
            log.error("AWS SES send failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new EmailSendException("Failed to send email via AWS SES: " + e.awsErrorDetails().errorMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error sending email via AWS SES", e);
            throw new EmailSendException("Unexpected error sending email via AWS SES", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return provider != null
                && provider.getConfigurationMap() != null
                && provider.getConfigurationMap().containsKey("accessKey")
                && provider.getConfigurationMap().containsKey("secretKey");
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS_SES;
    }
}
