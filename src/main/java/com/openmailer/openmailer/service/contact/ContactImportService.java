package com.openmailer.openmailer.service.contact;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactRepository;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for importing contacts from CSV files.
 * Handles parsing, validation, duplicate detection, and bulk insertion.
 */
@Service
@Transactional
public class ContactImportService {

    private static final Logger log = LoggerFactory.getLogger(ContactImportService.class);

    private static final int BATCH_SIZE = 100;
    private static final Map<String, ImportJob> importJobs = new ConcurrentHashMap<>();

    private final ContactRepository contactRepository;
    private final ContactListMembershipService membershipService;
    private final EmailValidator emailValidator;

    @Autowired
    public ContactImportService(
            ContactRepository contactRepository,
            ContactListMembershipService membershipService) {
        this.contactRepository = contactRepository;
        this.membershipService = membershipService;
        this.emailValidator = EmailValidator.getInstance();
    }

    /**
     * Starts an asynchronous import job from a CSV file.
     *
     * @param file the CSV file
     * @param user the user importing contacts
     * @param listId optional list to add contacts to
     * @param skipDuplicates whether to skip or update duplicates
     * @return import job ID
     */
    public String startImport(MultipartFile file, User user, String listId, boolean skipDuplicates) {
        String jobId = UUID.randomUUID().toString();

        ImportJob job = new ImportJob();
        job.setJobId(jobId);
        job.setUserId(user.getId());
        job.setListId(listId);
        job.setStatus("PROCESSING");
        job.setStartedAt(LocalDateTime.now());

        importJobs.put(jobId, job);

        // Start async processing
        processImportAsync(file, user, listId, skipDuplicates, job);

        return jobId;
    }

    /**
     * Processes the import asynchronously.
     */
    @Async
    public void processImportAsync(MultipartFile file, User user, String listId, boolean skipDuplicates, ImportJob job) {
        try {
            log.info("Starting import job {} for user {}", job.getJobId(), user.getId());

            List<Map<String, String>> records = parseCSV(file);
            job.setTotalRows(records.size());

            List<Contact> contactsToSave = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < records.size(); i++) {
                Map<String, String> row = records.get(i);

                try {
                    Contact contact = processRow(row, user, skipDuplicates);
                    if (contact != null) {
                        contactsToSave.add(contact);
                    }

                    // Batch insert
                    if (contactsToSave.size() >= BATCH_SIZE) {
                        saveContacts(contactsToSave, listId);
                        job.setImportedCount(job.getImportedCount() + contactsToSave.size());
                        contactsToSave.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    job.setErrorCount(job.getErrorCount() + 1);
                }
            }

            // Save remaining contacts
            if (!contactsToSave.isEmpty()) {
                saveContacts(contactsToSave, listId);
                job.setImportedCount(job.getImportedCount() + contactsToSave.size());
            }

            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            job.setErrors(errors);

            log.info("Import job {} completed. Imported: {}, Errors: {}",
                    job.getJobId(), job.getImportedCount(), job.getErrorCount());

        } catch (Exception e) {
            log.error("Import job {} failed: {}", job.getJobId(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setCompletedAt(LocalDateTime.now());
            job.getErrors().add("Import failed: " + e.getMessage());
        }
    }

    /**
     * Parses a CSV file and returns records as maps.
     */
    private List<Map<String, String>> parseCSV(MultipartFile file) throws IOException, CsvException {
        List<Map<String, String>> records = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // First row is headers
            String[] headers = rows.get(0);

            // Validate required headers
            boolean hasEmail = Arrays.asList(headers).stream()
                    .anyMatch(h -> h.equalsIgnoreCase("email"));
            if (!hasEmail) {
                throw new IllegalArgumentException("CSV must contain 'email' column");
            }

            // Parse data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] values = rows.get(i);
                Map<String, String> record = new HashMap<>();

                for (int j = 0; j < headers.length && j < values.length; j++) {
                    record.put(headers[j].toLowerCase(), values[j]);
                }

                records.add(record);
            }
        }

        return records;
    }

    /**
     * Processes a single CSV row into a Contact.
     */
    private Contact processRow(Map<String, String> row, User user, boolean skipDuplicates) {
        String email = row.get("email");

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        email = email.trim().toLowerCase();

        // Validate email format
        if (!emailValidator.isValid(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        // Check for duplicates
        Optional<Contact> existing = contactRepository.findByEmailAndUser_Id(email, user.getId());

        if (existing.isPresent()) {
            if (skipDuplicates) {
                return null; // Skip this contact
            } else {
                // Update existing contact
                Contact contact = existing.get();
                updateContactFromRow(contact, row);
                return contact;
            }
        }

        // Create new contact
        Contact contact = new Contact();
        contact.setUser(user);
        contact.setEmail(email);
        contact.setStatus("SUBSCRIBED");
        updateContactFromRow(contact, row);

        return contact;
    }

    /**
     * Updates contact fields from CSV row.
     */
    private void updateContactFromRow(Contact contact, Map<String, String> row) {
        if (row.containsKey("firstname") || row.containsKey("first_name")) {
            String firstName = row.getOrDefault("firstname", row.get("first_name"));
            if (firstName != null && !firstName.isEmpty()) {
                contact.setFirstName(firstName.trim());
            }
        }

        if (row.containsKey("lastname") || row.containsKey("last_name")) {
            String lastName = row.getOrDefault("lastname", row.get("last_name"));
            if (lastName != null && !lastName.isEmpty()) {
                contact.setLastName(lastName.trim());
            }
        }

        if (row.containsKey("status")) {
            String status = row.get("status");
            if (status != null && !status.isEmpty()) {
                contact.setStatus(status.toUpperCase());
            }
        }

        if (row.containsKey("source")) {
            String source = row.get("source");
            if (source != null && !source.isEmpty()) {
                contact.setSource(source.trim());
            }
        }

        // Handle custom fields (any column not in standard fields)
        Map<String, Object> customFields = new HashMap<>();
        Set<String> standardFields = Set.of("email", "firstname", "first_name", "lastname", "last_name", "status", "source");

        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (!standardFields.contains(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                customFields.put(entry.getKey(), entry.getValue());
            }
        }

        if (!customFields.isEmpty()) {
            contact.setCustomFields(customFields);
        }
    }

    /**
     * Saves contacts in batch and optionally adds them to a list.
     */
    private void saveContacts(List<Contact> contacts, String listId) {
        List<Contact> saved = contactRepository.saveAll(contacts);

        // Add to list if specified
        if (listId != null && !listId.isEmpty()) {
            List<String> contactIds = saved.stream()
                    .map(Contact::getId)
                    .toList();
            membershipService.addContactsToList(contactIds, listId);
        }
    }

    /**
     * Gets the status of an import job.
     *
     * @param jobId the job ID
     * @return import job status
     */
    public ImportJob getImportStatus(String jobId) {
        ImportJob job = importJobs.get(jobId);
        if (job == null) {
            throw new RuntimeException("Import job not found: " + jobId);
        }
        return job;
    }

    /**
     * Validates a CSV file without importing.
     *
     * @param file the CSV file
     * @return validation result
     */
    public ValidationResult validateCSV(MultipartFile file) {
        ValidationResult result = new ValidationResult();

        try {
            List<Map<String, String>> records = parseCSV(file);
            result.setValid(true);
            result.setTotalRows(records.size());

            Set<String> emails = new HashSet<>();
            int duplicatesInFile = 0;
            int invalidEmails = 0;

            for (Map<String, String> row : records) {
                String email = row.get("email");

                if (email == null || email.trim().isEmpty()) {
                    invalidEmails++;
                    continue;
                }

                email = email.trim().toLowerCase();

                if (!emailValidator.isValid(email)) {
                    invalidEmails++;
                    continue;
                }

                if (emails.contains(email)) {
                    duplicatesInFile++;
                } else {
                    emails.add(email);
                }
            }

            result.setDuplicatesInFile(duplicatesInFile);
            result.setInvalidEmails(invalidEmails);
            result.setValidRows(records.size() - invalidEmails);

        } catch (Exception e) {
            result.setValid(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Import job tracking.
     */
    public static class ImportJob {
        private String jobId;
        private String userId;
        private String listId;
        private String status;
        private int totalRows;
        private int importedCount = 0;
        private int errorCount = 0;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private List<String> errors = new ArrayList<>();

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getListId() {
            return listId;
        }

        public void setListId(String listId) {
            this.listId = listId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public void setImportedCount(int importedCount) {
            this.importedCount = importedCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }

    /**
     * Validation result for CSV files.
     */
    public static class ValidationResult {
        private boolean valid;
        private int totalRows;
        private int validRows;
        private int invalidEmails;
        private int duplicatesInFile;
        private String error;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getValidRows() {
            return validRows;
        }

        public void setValidRows(int validRows) {
            this.validRows = validRows;
        }

        public int getInvalidEmails() {
            return invalidEmails;
        }

        public void setInvalidEmails(int invalidEmails) {
            this.invalidEmails = invalidEmails;
        }

        public int getDuplicatesInFile() {
            return duplicatesInFile;
        }

        public void setDuplicatesInFile(int duplicatesInFile) {
            this.duplicatesInFile = duplicatesInFile;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
