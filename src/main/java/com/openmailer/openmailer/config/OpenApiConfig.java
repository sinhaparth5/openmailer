package com.openmailer.openmailer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 * Access the UI at: /swagger-ui.html or /swagger-ui/index.html
 * Access the API docs at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openMailerOpenAPI() {
        // Security scheme for JWT Bearer token
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Bearer token authentication. Format: 'Bearer {token}'");

        // Security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("OpenMailer API")
                        .description("RESTful API for OpenMailer - Email Marketing Platform\n\n" +
                                "## Authentication\n" +
                                "Most endpoints require JWT authentication. Use the `/api/auth/login` endpoint to obtain a token, " +
                                "then include it in the `Authorization` header as `Bearer {token}`.\n\n" +
                                "## Features\n" +
                                "- **User Management**: Register, login, and manage user accounts\n" +
                                "- **Contacts**: Manage email contacts and contact lists\n" +
                                "- **Templates**: Create and manage email templates with variable substitution\n" +
                                "- **Campaigns**: Create, schedule, and send email campaigns\n" +
                                "- **Providers**: Configure email providers (AWS SES, SendGrid, SMTP)\n" +
                                "- **Domains**: Manage domains with SPF, DKIM, and DMARC verification\n" +
                                "- **Analytics**: Track email performance and engagement")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OpenMailer Team")
                                .email("support@openmailer.com")
                                .url("https://openmailer.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.openmailer.com")
                                .description("Production Server (if applicable)")
                ))
                // Add security scheme to the components
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                // Apply security globally to all endpoints
                .addSecurityItem(securityRequirement);
    }
}
