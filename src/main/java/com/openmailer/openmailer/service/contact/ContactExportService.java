package com.openmailer.openmailer.service.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for exporting contacts to CSV and JSON formats.
 * Supports filtering by list, segment, status, and custom fields.
 */
@Service
@Transactional(readOnly = true)
public class ContactExportService {

    private static final Logger log = LoggerFactory.getLogger(ContactExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContactRepository contactRepository;
    private final ContactListMembershipService membershipService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ContactExportService(
            ContactRepository contactRepository,
            ContactListMembershipService membershipService) {
        this.contactRepository = contactRepository;
        this.membershipService = membershipService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Exports contacts to CSV format.
     *
     * @param userId the user ID
     * @param options export options (filters, fields, etc.)
     * @return CSV as byte array
     */
    public byte[] exportToCSV(String userId, ExportOptions options) {
        log.info("Exporting contacts to CSV for user: {}", userId);

        try {
            List<Contact> contacts = getContactsForExport(userId, options);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // Write headers
            String[] headers = getCSVHeaders(options);
            writer.writeNext(headers);

            // Write data rows
            for (Contact contact : contacts) {
                String[] row = contactToCSVRow(contact, options);
                writer.writeNext(row);
            }

            writer.close();

            log.info("Exported {} contacts to CSV", contacts.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export contacts to CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export contacts to CSV", e);
        }
    }

    /**
     * Exports contacts to JSON format.
     *
     * @param userId the user ID
     * @param options export options
     * @return JSON as byte array
     */
    public byte[] exportToJSON(String userId, ExportOptions options) {
        log.info("Exporting contacts to JSON for user: {}", userId);

        try {
            List<Contact> contacts = getContactsForExport(userId, options);

            List<Map<String, Object>> exportData = new ArrayList<>();

            for (Contact contact : contacts) {
                Map<String, Object> contactMap = contactToMap(contact, options);
                exportData.add(contactMap);
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            log.info("Exported {} contacts to JSON", contacts.size());
            return json.getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to export contacts to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export contacts to JSON", e);
        }
    }

    /**
     * Gets contacts for export based on filters.
     */
    private List<Contact> getContactsForExport(String userId, ExportOptions options) {
        List<Contact> contacts;

        if (options.getListId() != null) {
            // Export contacts from a specific list
            List<String> contactIds = membershipService.getContactIdsByList(options.getListId());
            contacts = contactRepository.findAllById(contactIds);

            // Filter by user
            contacts = contacts.stream()
                    .filter(c -> userId.equals(c.getUserId()))
                    .toList();

        } else if (options.getStatus() != null) {
            // Export by status
            Page<Contact> page = contactRepository.findByUser_IdAndStatus(
                    userId,
                    options.getStatus(),
                    PageRequest.of(0, Integer.MAX_VALUE)
            );
            contacts = page.getContent();

        } else {
            // Export all contacts
            contacts = contactRepository.findByUser_Id(userId);
        }

        return contacts;
    }

    /**
     * Gets CSV headers based on export options.
     */
    private String[] getCSVHeaders(ExportOptions options) {
        List<String> headers = new ArrayList<>();

        headers.add("Email");

        if (options.isIncludeFirstName()) {
            headers.add("First Name");
        }
        if (options.isIncludeLastName()) {
            headers.add("Last Name");
        }
        if (options.isIncludeStatus()) {
            headers.add("Status");
        }
        if (options.isIncludeSource()) {
            headers.add("Source");
        }
        if (options.isIncludeTags()) {
            headers.add("Tags");
        }
        if (options.isIncludeSubscribedAt()) {
            headers.add("Subscribed At");
        }
        if (options.isIncludeCreatedAt()) {
            headers.add("Created At");
        }
        if (options.isIncludeCustomFields()) {
            headers.add("Custom Fields");
        }

        return headers.toArray(new String[0]);
    }

    /**
     * Converts a contact to a CSV row.
     */
    private String[] contactToCSVRow(Contact contact, ExportOptions options) {
        List<String> values = new ArrayList<>();

        values.add(contact.getEmail());

        if (options.isIncludeFirstName()) {
            values.add(contact.getFirstName() != null ? contact.getFirstName() : "");
        }
        if (options.isIncludeLastName()) {
            values.add(contact.getLastName() != null ? contact.getLastName() : "");
        }
        if (options.isIncludeStatus()) {
            values.add(contact.getStatus() != null ? contact.getStatus() : "");
        }
        if (options.isIncludeSource()) {
            values.add(contact.getSource() != null ? contact.getSource() : "");
        }
        if (options.isIncludeTags()) {
            String tags = contact.getTags() != null ? String.join(", ", contact.getTags()) : "";
            values.add(tags);
        }
        if (options.isIncludeSubscribedAt()) {
            String subscribedAt = contact.getSubscribedAt() != null
                    ? contact.getSubscribedAt().format(DATE_FORMATTER)
                    : "";
            values.add(subscribedAt);
        }
        if (options.isIncludeCreatedAt()) {
            String createdAt = contact.getCreatedAt() != null
                    ? contact.getCreatedAt().format(DATE_FORMATTER)
                    : "";
            values.add(createdAt);
        }
        if (options.isIncludeCustomFields() && contact.getCustomFields() != null) {
            try {
                String customFields = objectMapper.writeValueAsString(contact.getCustomFields());
                values.add(customFields);
            } catch (Exception e) {
                values.add("{}");
            }
        }

        return values.toArray(new String[0]);
    }

    /**
     * Converts a contact to a map for JSON export.
     */
    private Map<String, Object> contactToMap(Contact contact, ExportOptions options) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("email", contact.getEmail());

        if (options.isIncludeFirstName()) {
            map.put("firstName", contact.getFirstName());
        }
        if (options.isIncludeLastName()) {
            map.put("lastName", contact.getLastName());
        }
        if (options.isIncludeStatus()) {
            map.put("status", contact.getStatus());
        }
        if (options.isIncludeSource()) {
            map.put("source", contact.getSource());
        }
        if (options.isIncludeTags() && contact.getTags() != null) {
            map.put("tags", Arrays.asList(contact.getTags()));
        }
        if (options.isIncludeSubscribedAt()) {
            map.put("subscribedAt", contact.getSubscribedAt() != null
                    ? contact.getSubscribedAt().format(DATE_FORMATTER)
                    : null);
        }
        if (options.isIncludeCreatedAt()) {
            map.put("createdAt", contact.getCreatedAt() != null
                    ? contact.getCreatedAt().format(DATE_FORMATTER)
                    : null);
        }
        if (options.isIncludeCustomFields() && contact.getCustomFields() != null) {
            map.put("customFields", contact.getCustomFields());
        }

        return map;
    }

    /**
     * Export options configuration.
     */
    public static class ExportOptions {
        private String listId;
        private String segmentId;
        private String status;
        private boolean includeFirstName = true;
        private boolean includeLastName = true;
        private boolean includeStatus = true;
        private boolean includeSource = false;
        private boolean includeTags = false;
        private boolean includeSubscribedAt = false;
        private boolean includeCreatedAt = false;
        private boolean includeCustomFields = false;

        public String getListId() {
            return listId;
        }

        public void setListId(String listId) {
            this.listId = listId;
        }

        public String getSegmentId() {
            return segmentId;
        }

        public void setSegmentId(String segmentId) {
            this.segmentId = segmentId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isIncludeFirstName() {
            return includeFirstName;
        }

        public void setIncludeFirstName(boolean includeFirstName) {
            this.includeFirstName = includeFirstName;
        }

        public boolean isIncludeLastName() {
            return includeLastName;
        }

        public void setIncludeLastName(boolean includeLastName) {
            this.includeLastName = includeLastName;
        }

        public boolean isIncludeStatus() {
            return includeStatus;
        }

        public void setIncludeStatus(boolean includeStatus) {
            this.includeStatus = includeStatus;
        }

        public boolean isIncludeSource() {
            return includeSource;
        }

        public void setIncludeSource(boolean includeSource) {
            this.includeSource = includeSource;
        }

        public boolean isIncludeTags() {
            return includeTags;
        }

        public void setIncludeTags(boolean includeTags) {
            this.includeTags = includeTags;
        }

        public boolean isIncludeSubscribedAt() {
            return includeSubscribedAt;
        }

        public void setIncludeSubscribedAt(boolean includeSubscribedAt) {
            this.includeSubscribedAt = includeSubscribedAt;
        }

        public boolean isIncludeCreatedAt() {
            return includeCreatedAt;
        }

        public void setIncludeCreatedAt(boolean includeCreatedAt) {
            this.includeCreatedAt = includeCreatedAt;
        }

        public boolean isIncludeCustomFields() {
            return includeCustomFields;
        }

        public void setIncludeCustomFields(boolean includeCustomFields) {
            this.includeCustomFields = includeCustomFields;
        }
    }
}
