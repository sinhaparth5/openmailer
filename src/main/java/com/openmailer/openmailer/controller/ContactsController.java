package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactListRepository;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.contact.ContactImportService;
import com.openmailer.openmailer.service.contact.ContactService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/contacts")
public class ContactsController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final ContactService contactService;
    private final ContactImportService contactImportService;
    private final ContactListRepository contactListRepository;

    public ContactsController(
        ContactService contactService,
        ContactImportService contactImportService,
        ContactListRepository contactListRepository
    ) {
        this.contactService = contactService;
        this.contactImportService = contactImportService;
        this.contactListRepository = contactListRepository;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) String search,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        String userId = userDetails.getUser().getId();
        List<Contact> userContacts = contactService.findByUserId(userId);

        model.addAttribute("pageTitle", "Contacts - OpenMailer");
        model.addAttribute("totalContacts", contactService.countByUserId(userId));
        model.addAttribute("subscribedContacts", contactService.countByStatus(userId, "SUBSCRIBED"));
        model.addAttribute("unsubscribedContacts", contactService.countByStatus(userId, "UNSUBSCRIBED"));
        model.addAttribute("bouncedContacts", contactService.countByStatus(userId, "BOUNCED"));
        model.addAttribute("statusFilters", List.of(
            Map.of("value", "", "label", "All Statuses"),
            Map.of("value", "SUBSCRIBED", "label", "Subscribed"),
            Map.of("value", "UNSUBSCRIBED", "label", "Unsubscribed"),
            Map.of("value", "PENDING", "label", "Pending"),
            Map.of("value", "BOUNCED", "label", "Bounced")
        ));
        model.addAttribute("tagFilters", buildTagFilters(userContacts));
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("currentTag", tag != null ? tag : "");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("contacts", userContacts.stream()
            .map(this::toSummaryView)
            .filter(contact -> status == null || status.isBlank() || status.equals(contact.status()))
            .filter(contact -> tag == null || tag.isBlank() || contact.tags().contains(tag))
            .filter(contact -> matchesSearch(contact, search))
            .sorted(Comparator.comparing(ContactSummaryView::sortDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList());
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);
        model.addAttribute("pages", Collections.singletonList(1));

        return "contacts/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("pageTitle", "Add Contact - OpenMailer");
        model.addAttribute("mode", "add");
        model.addAttribute("contactForm", new ContactForm());
        return "contacts/form";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @ModelAttribute ContactForm contactForm,
        RedirectAttributes redirectAttributes
    ) {
        Contact saved = contactService.createContact(toEntity(contactForm, userDetails.getUser()));
        redirectAttributes.addFlashAttribute("successMessage", "Contact created successfully.");
        return "redirect:/contacts/" + saved.getId();
    }

    @GetMapping("/import")
    public String importContacts(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        model.addAttribute("pageTitle", "Import Contacts - OpenMailer");
        model.addAttribute("hasImportCapability", contactImportService != null);
        model.addAttribute("listOptions", contactListRepository.findByUser_Id(userDetails.getUser().getId()));
        return "contacts/import";
    }

    @GetMapping("/{id}")
    public String view(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        Contact contact = contactService.findByIdAndUserId(id, userDetails.getUser().getId());
        model.addAttribute("pageTitle", "Contact Details - OpenMailer");
        model.addAttribute("contact", toDetailView(contact));
        return "contacts/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        model.addAttribute("pageTitle", "Edit Contact - OpenMailer");
        model.addAttribute("mode", "edit");
        model.addAttribute("contactForm", toForm(contactService.findByIdAndUserId(id, userDetails.getUser().getId())));
        return "contacts/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @ModelAttribute ContactForm contactForm,
        RedirectAttributes redirectAttributes
    ) {
        contactService.updateContact(id, userDetails.getUser().getId(), toEntity(contactForm, userDetails.getUser()));
        redirectAttributes.addFlashAttribute("successMessage", "Contact updated successfully.");
        return "redirect:/contacts/" + id;
    }

    private List<Map<String, String>> buildTagFilters(List<Contact> contacts) {
        Set<String> tags = contacts.stream()
            .map(Contact::getTags)
            .filter(Objects::nonNull)
            .flatMap(Arrays::stream)
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toCollection(TreeSet::new));

        List<Map<String, String>> filters = new ArrayList<>();
        filters.add(Map.of("value", "", "label", "All Tags"));
        tags.forEach(tag -> filters.add(Map.of("value", tag, "label", tag)));
        return filters;
    }

    private boolean matchesSearch(ContactSummaryView contact, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase(Locale.ROOT);
        return contact.name().toLowerCase(Locale.ROOT).contains(query)
            || contact.email().toLowerCase(Locale.ROOT).contains(query);
    }

    private ContactSummaryView toSummaryView(Contact contact) {
        LocalDateTime sortDate = Optional.ofNullable(contact.getSubscribedAt()).orElse(contact.getCreatedAt());
        return new ContactSummaryView(
            contact.getId(),
            buildContactName(contact),
            contact.getEmail(),
            contact.getStatus(),
            normalizeTags(contact.getTags()),
            extractCustomField(contact, "company"),
            formatDate(sortDate),
            sortDate
        );
    }

    private ContactDetailView toDetailView(Contact contact) {
        return new ContactDetailView(
            contact.getId(),
            buildContactName(contact),
            contact.getEmail(),
            contact.getStatus(),
            normalizeTags(contact.getTags()),
            extractCustomField(contact, "company"),
            extractCustomField(contact, "phone"),
            extractCustomField(contact, "country"),
            formatDate(contact.getSubscribedAt()),
            formatDate(contact.getCreatedAt()),
            formatDate(contact.getConfirmedAt()),
            formatDate(contact.getUpdatedAt()),
            contact.getSource(),
            Boolean.TRUE.equals(contact.getEmailVerified()),
            Boolean.TRUE.equals(contact.getGdprConsent()),
            contact.getBounceCount() != null ? contact.getBounceCount() : 0,
            contact.getComplaintCount() != null ? contact.getComplaintCount() : 0,
            contact.getUnsubscribeReason(),
            contact.getNotes()
        );
    }

    private ContactForm toForm(Contact contact) {
        ContactForm form = new ContactForm();
        form.id = contact.getId();
        form.firstName = contact.getFirstName();
        form.lastName = contact.getLastName();
        form.email = contact.getEmail();
        form.company = extractCustomField(contact, "company");
        form.phone = extractCustomField(contact, "phone");
        form.country = extractCustomField(contact, "country");
        form.status = contact.getStatus();
        form.tags = normalizeTags(contact.getTags());
        form.notes = contact.getNotes();
        form.gdprConsent = Boolean.TRUE.equals(contact.getGdprConsent());
        return form;
    }

    private Contact toEntity(ContactForm form, User user) {
        Contact contact = new Contact();
        contact.setUser(user);
        contact.setEmail(form.email != null ? form.email.trim() : null);
        contact.setFirstName(blankToNull(form.firstName));
        contact.setLastName(blankToNull(form.lastName));
        contact.setStatus(blankToNull(form.status) != null ? form.status : "SUBSCRIBED");
        contact.setNotes(blankToNull(form.notes));
        contact.setGdprConsent(form.gdprConsent);
        if (form.tags != null && !form.tags.isEmpty()) {
            contact.setTags(form.tags.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toArray(String[]::new));
        }

        Map<String, Object> customFields = new HashMap<>();
        if (blankToNull(form.company) != null) {
            customFields.put("company", form.company.trim());
        }
        if (blankToNull(form.phone) != null) {
            customFields.put("phone", form.phone.trim());
        }
        if (blankToNull(form.country) != null) {
            customFields.put("country", form.country.trim());
        }
        if (!customFields.isEmpty()) {
            contact.setCustomFields(customFields);
        }
        return contact;
    }

    private String buildContactName(Contact contact) {
        String fullName = Stream.of(contact.getFirstName(), contact.getLastName())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.joining(" "));
        return fullName.isBlank() ? contact.getEmail() : fullName;
    }

    private List<String> normalizeTags(String[] tags) {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }

    private String extractCustomField(Contact contact, String key) {
        if (contact.getCustomFields() == null) {
            return null;
        }
        Object value = contact.getCustomFields().get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMAT) : "Not recorded";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record ContactSummaryView(
        String id,
        String name,
        String email,
        String status,
        List<String> tags,
        String company,
        String subscribedDate,
        LocalDateTime sortDate
    ) {}

    private record ContactDetailView(
        String id,
        String name,
        String email,
        String status,
        List<String> tags,
        String company,
        String phone,
        String country,
        String subscribedDate,
        String createdDate,
        String confirmedDate,
        String updatedDate,
        String source,
        boolean emailVerified,
        boolean gdprConsent,
        int bounceCount,
        int complaintCount,
        String unsubscribeReason,
        String notes
    ) {}

    public static class ContactForm {
        public String id;
        public String firstName;
        public String lastName;
        public String email;
        public String company;
        public String phone;
        public String country;
        public String status = "SUBSCRIBED";
        public List<String> tags = Collections.emptyList();
        public String notes;
        public boolean gdprConsent;
    }
}
