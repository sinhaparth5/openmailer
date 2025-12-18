package com.openmailer.openmailer.service.template;

import com.openmailer.openmailer.model.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for rendering email templates with variable substitution
 * Supports variables like {{first_name}}, {{last_name}}, {{custom.field}}, etc.
 */
@Service
public class TemplateRendererService {

    private static final Logger log = LoggerFactory.getLogger(TemplateRendererService.class);

    // Pattern to match {{variable}} or {{custom.field}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*\\}\\}");

    /**
     * Renders a template by substituting variables with contact data
     *
     * @param template The template content with {{variables}}
     * @param contact  The contact entity containing data
     * @return Rendered template with variables replaced
     */
    public String render(String template, Contact contact) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Map<String, String> variables = buildVariablesFromContact(contact);
        return render(template, variables);
    }

    /**
     * Renders a template by substituting variables with provided data
     *
     * @param template  The template content with {{variables}}
     * @param variables Map of variable names to values
     * @return Rendered template with variables replaced
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (variables == null || variables.isEmpty()) {
            // Remove all unsubstituted variables
            return removeUnsubstitutedVariables(template);
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String replacement = variables.getOrDefault(variableName, "");

            // Escape special characters in replacement for regex
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Builds a variables map from a Contact entity
     *
     * @param contact The contact entity
     * @return Map of variable names to values
     */
    private Map<String, String> buildVariablesFromContact(Contact contact) {
        Map<String, String> variables = new HashMap<>();

        if (contact == null) {
            return variables;
        }

        // Standard fields
        variables.put("email", contact.getEmail() != null ? contact.getEmail() : "");
        variables.put("first_name", contact.getFirstName() != null ? contact.getFirstName() : "");
        variables.put("firstName", contact.getFirstName() != null ? contact.getFirstName() : "");
        variables.put("last_name", contact.getLastName() != null ? contact.getLastName() : "");
        variables.put("lastName", contact.getLastName() != null ? contact.getLastName() : "");

        // Full name
        String fullName = buildFullName(contact.getFirstName(), contact.getLastName());
        variables.put("full_name", fullName);
        variables.put("fullName", fullName);

        // Status
        if (contact.getStatus() != null) {
            variables.put("status", contact.getStatus());
        }

        // Custom fields (prefixed with "custom.")
        if (contact.getCustomFields() != null && !contact.getCustomFields().isEmpty()) {
            for (Map.Entry<String, Object> entry : contact.getCustomFields().entrySet()) {
                String key = "custom." + entry.getKey();
                variables.put(key, entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }

        // Tags (comma-separated)
        if (contact.getTags() != null && contact.getTags().length > 0) {
            variables.put("tags", String.join(", ", contact.getTags()));
        }

        // Current date/time
        variables.put("current_date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        variables.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

        return variables;
    }

    /**
     * Builds a full name from first and last name
     *
     * @param firstName First name
     * @param lastName  Last name
     * @return Full name or empty string
     */
    private String buildFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "";
    }

    /**
     * Removes all unsubstituted variables from the template
     * Useful when no contact data is provided
     *
     * @param template The template content
     * @return Template with all {{variables}} removed
     */
    private String removeUnsubstitutedVariables(String template) {
        return VARIABLE_PATTERN.matcher(template).replaceAll("");
    }

    /**
     * Extracts all variable names from a template
     *
     * @param template The template content
     * @return Set of variable names found in the template
     */
    public java.util.Set<String> extractVariables(String template) {
        java.util.Set<String> variables = new java.util.HashSet<>();

        if (template == null || template.isEmpty()) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }

        return variables;
    }

    /**
     * Validates that all required variables are present in the provided data
     *
     * @param template  The template content
     * @param variables The variables map
     * @return true if all required variables are present, false otherwise
     */
    public boolean validateVariables(String template, Map<String, String> variables) {
        java.util.Set<String> requiredVariables = extractVariables(template);

        if (requiredVariables.isEmpty()) {
            return true; // No variables required
        }

        if (variables == null || variables.isEmpty()) {
            return false; // Variables required but none provided
        }

        // Check if all required variables are present
        for (String required : requiredVariables) {
            if (!variables.containsKey(required)) {
                log.warn("Missing required variable: {}", required);
                return false;
            }
        }

        return true;
    }

    /**
     * Renders subject line with variable substitution
     *
     * @param subject The subject line with {{variables}}
     * @param contact The contact entity
     * @return Rendered subject line
     */
    public String renderSubject(String subject, Contact contact) {
        return render(subject, contact);
    }

    /**
     * Renders preview text with variable substitution
     *
     * @param previewText The preview text with {{variables}}
     * @param contact     The contact entity
     * @return Rendered preview text
     */
    public String renderPreviewText(String previewText, Contact contact) {
        return render(previewText, contact);
    }

    /**
     * Adds tracking pixel to HTML body for open tracking
     *
     * @param htmlBody   The HTML body content
     * @param trackingId The unique tracking ID
     * @param baseUrl    The base URL for tracking endpoint
     * @return HTML body with tracking pixel
     */
    public String addTrackingPixel(String htmlBody, String trackingId, String baseUrl) {
        if (htmlBody == null || htmlBody.isEmpty()) {
            return htmlBody;
        }

        String trackingPixel = String.format(
                "<img src=\"%s/track/open/%s\" width=\"1\" height=\"1\" alt=\"\" style=\"display:none\" />",
                baseUrl, trackingId
        );

        // Try to insert before closing </body> tag
        if (htmlBody.toLowerCase().contains("</body>")) {
            return htmlBody.replaceFirst("(?i)</body>", trackingPixel + "</body>");
        } else {
            // Append to the end if no </body> tag found
            return htmlBody + trackingPixel;
        }
    }

    /**
     * Replaces all links in HTML with tracking links
     *
     * @param htmlBody   The HTML body content
     * @param linkMap    Map of original URLs to tracking short codes
     * @param baseUrl    The base URL for tracking endpoint
     * @return HTML body with tracking links
     */
    public String replaceLinksWithTracking(String htmlBody, Map<String, String> linkMap, String baseUrl) {
        if (htmlBody == null || htmlBody.isEmpty() || linkMap == null || linkMap.isEmpty()) {
            return htmlBody;
        }

        String result = htmlBody;

        for (Map.Entry<String, String> entry : linkMap.entrySet()) {
            String originalUrl = entry.getKey();
            String shortCode = entry.getValue();
            String trackingUrl = baseUrl + "/track/click/" + shortCode;

            // Replace in href attributes
            result = result.replaceAll(
                    "href=[\"']" + Pattern.quote(originalUrl) + "[\"']",
                    "href=\"" + trackingUrl + "\""
            );
        }

        return result;
    }
}
