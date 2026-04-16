package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.ContactListMembership;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactListRepository;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.contact.ContactImportService;
import com.openmailer.openmailer.service.contact.ContactListService;
import com.openmailer.openmailer.service.contact.ContactListMembershipService;
import com.openmailer.openmailer.service.contact.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    private final ContactListService contactListService;
    private final ContactListMembershipService membershipService;
    private final ContactListRepository contactListRepository;

    public ContactsController(
        ContactService contactService,
        ContactImportService contactImportService,
        ContactListService contactListService,
        ContactListMembershipService membershipService,
        ContactListRepository contactListRepository
    ) {
        this.contactService = contactService;
        this.contactImportService = contactImportService;
        this.contactListService = contactListService;
        this.membershipService = membershipService;
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

    @GetMapping("/lists")
    public String lists(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        String userId = userDetails.getUser().getId();
        List<ContactList> userLists = contactListService.findByUserId(userId);

        model.addAttribute("pageTitle", "Contact Lists - OpenMailer");
        model.addAttribute("contactListForm", new ContactListForm());
        model.addAttribute("lists", userLists.stream()
            .map(this::toListSummaryView)
            .sorted(Comparator.comparing(ContactListSummaryView::name, String.CASE_INSENSITIVE_ORDER))
            .toList());
        model.addAttribute("totalLists", userLists.size());
        model.addAttribute("listsWithContacts", userLists.stream()
            .map(this::toListSummaryView)
            .filter(list -> list.totalContacts() > 0)
            .count());
        model.addAttribute("contactsWithoutLists", countContactsWithoutLists(userId));

        return "contacts/lists";
    }

    @PostMapping("/lists")
    public String createList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("contactListForm") ContactListForm contactListForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        String userId = userDetails.getUser().getId();
        if (bindingResult.hasErrors()) {
            populateContactListPage(model, userId, contactListForm);
            return "contacts/lists";
        }

        try {
            ContactList list = new ContactList();
            list.setUser(userDetails.getUser());
            list.setName(contactListForm.name != null ? contactListForm.name.trim() : null);
            list.setDescription(blankToNull(contactListForm.description));
            list.setDoubleOptInEnabled(contactListForm.doubleOptInEnabled);
            contactListService.createContactList(list);
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            populateContactListPage(model, userId, contactListForm);
            return "contacts/lists";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Contact list created successfully.");
        return "redirect:/contacts/lists";
    }

    @GetMapping("/lists/{id}")
    public String viewList(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        Model model
    ) {
        String userId = userDetails.getUser().getId();
        ContactList list = contactListService.findByIdAndUserId(id, userId);
        List<ContactListMembership> memberships = membershipService.findByList(id);

        model.addAttribute("pageTitle", list.getName() + " - Contact List - OpenMailer");
        model.addAttribute("contactList", toListDetailView(list));
        model.addAttribute("contactListForm", toListForm(list));
        model.addAttribute("contacts", memberships.stream()
            .map(this::toListMemberView)
            .sorted(Comparator.comparing(ContactListMemberView::name, String.CASE_INSENSITIVE_ORDER))
            .toList());

        return "contacts/list-view";
    }

    @PostMapping("/lists/{id}/edit")
    public String updateList(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("contactListForm") ContactListForm contactListForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        String userId = userDetails.getUser().getId();
        ContactList existingList = contactListService.findByIdAndUserId(id, userId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", existingList.getName() + " - Contact List - OpenMailer");
            model.addAttribute("contactList", toListDetailView(existingList));
            model.addAttribute("contacts", membershipService.findByList(id).stream()
                .map(this::toListMemberView)
                .sorted(Comparator.comparing(ContactListMemberView::name, String.CASE_INSENSITIVE_ORDER))
                .toList());
            return "contacts/list-view";
        }

        try {
            ContactList updatedList = new ContactList();
            updatedList.setName(contactListForm.name != null ? contactListForm.name.trim() : null);
            updatedList.setDescription(blankToNull(contactListForm.description));
            updatedList.setDoubleOptInEnabled(contactListForm.doubleOptInEnabled);
            contactListService.updateContactList(id, userId, updatedList);
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            ContactList refreshedList = contactListService.findByIdAndUserId(id, userId);
            model.addAttribute("pageTitle", refreshedList.getName() + " - Contact List - OpenMailer");
            model.addAttribute("contactList", toListDetailView(refreshedList));
            model.addAttribute("contacts", membershipService.findByList(id).stream()
                .map(this::toListMemberView)
                .sorted(Comparator.comparing(ContactListMemberView::name, String.CASE_INSENSITIVE_ORDER))
                .toList());
            return "contacts/list-view";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Contact list updated successfully.");
        return "redirect:/contacts/lists/" + id;
    }

    @PostMapping("/lists/{id}/delete")
    public String deleteList(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        String userId = userDetails.getUser().getId();
        contactListService.findByIdAndUserId(id, userId);
        membershipService.removeAllContactsFromList(id);
        contactListService.deleteContactList(id, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Contact list deleted successfully.");
        return "redirect:/contacts/lists";
    }

    @GetMapping("/add")
    public String add(Model model) {
        populateFormMetadata(model, "Add Contact - OpenMailer", "add");
        model.addAttribute("contactForm", new ContactForm());
        return "contacts/form";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("contactForm") ContactForm contactForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Add Contact - OpenMailer", "add");
            return "contacts/form";
        }

        Contact saved;
        try {
            saved = contactService.createContact(toEntity(contactForm, userDetails.getUser()));
            syncContactLists(saved.getId(), userDetails.getUser().getId(), contactForm.getSelectedListIds());
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            populateFormMetadata(model, "Add Contact - OpenMailer", "add");
            return "contacts/form";
        }
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
        populateFormMetadata(model, "Edit Contact - OpenMailer", "edit");
        model.addAttribute("contactForm", toForm(contactService.findByIdAndUserId(id, userDetails.getUser().getId())));
        return "contacts/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable String id,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute("contactForm") ContactForm contactForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateFormMetadata(model, "Edit Contact - OpenMailer", "edit");
            return "contacts/form";
        }

        try {
            contactService.updateContact(id, userDetails.getUser().getId(), toEntity(contactForm, userDetails.getUser()));
            syncContactLists(id, userDetails.getUser().getId(), contactForm.getSelectedListIds());
        } catch (ValidationException ex) {
            bindValidationError(bindingResult, ex);
            populateFormMetadata(model, "Edit Contact - OpenMailer", "edit");
            return "contacts/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Contact updated successfully.");
        return "redirect:/contacts/" + id;
    }

    @ModelAttribute
    public void populateFormOptions(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return;
        }
        String userId = userDetails.getUser().getId();
        model.addAttribute("tagFilters", buildTagFilters(contactService.findByUserId(userId)));
        model.addAttribute("listOptions", contactListRepository.findByUser_Id(userId));
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
        form.selectedListIds = membershipService.findByContact(contact.getId()).stream()
            .map(ContactListMembership::getListId)
            .toList();
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

    private void populateFormMetadata(Model model, String pageTitle, String mode) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("mode", mode);
    }

    private void populateContactListPage(Model model, String userId, ContactListForm contactListForm) {
        List<ContactList> userLists = contactListService.findByUserId(userId);
        model.addAttribute("pageTitle", "Contact Lists - OpenMailer");
        model.addAttribute("contactListForm", contactListForm);
        model.addAttribute("lists", userLists.stream()
            .map(this::toListSummaryView)
            .sorted(Comparator.comparing(ContactListSummaryView::name, String.CASE_INSENSITIVE_ORDER))
            .toList());
        model.addAttribute("totalLists", userLists.size());
        model.addAttribute("listsWithContacts", userLists.stream()
            .map(this::toListSummaryView)
            .filter(list -> list.totalContacts() > 0)
            .count());
        model.addAttribute("contactsWithoutLists", countContactsWithoutLists(userId));
    }

    private void syncContactLists(String contactId, String userId, List<String> selectedListIds) {
        List<String> existingListIds = membershipService.findByContact(contactId).stream()
            .map(ContactListMembership::getListId)
            .toList();
        List<String> requestedListIds = selectedListIds == null ? Collections.emptyList() : selectedListIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();

        for (String listId : requestedListIds) {
            ContactList list = contactListRepository.findByIdAndUser_Id(listId, userId)
                .orElseThrow(() -> new ValidationException("Selected list is invalid", "selectedListIds"));
            if (!membershipService.isContactInList(contactId, list.getId())) {
                ContactListMembership membership = new ContactListMembership();
                membership.setContactId(contactId);
                membership.setListId(list.getId());
                membership.setStatus("ACTIVE");
                membershipService.addContactToList(membership);
            }
        }

        existingListIds.stream()
            .filter(existingListId -> !requestedListIds.contains(existingListId))
            .forEach(existingListId -> membershipService.removeContactFromList(contactId, existingListId));

        Stream.concat(
                requestedListIds.stream(),
                existingListIds.stream()
            )
            .distinct()
            .forEach(listId -> refreshListStatistics(listId, userId));
    }

    private void bindValidationError(BindingResult bindingResult, ValidationException ex) {
        if (ex.getField() != null && !ex.getField().isBlank()) {
            bindingResult.rejectValue(ex.getField(), "validation", ex.getMessage());
            return;
        }
        bindingResult.reject("validation", ex.getMessage());
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

    private ContactListSummaryView toListSummaryView(ContactList list) {
        long totalContacts = membershipService.countByList(list.getId());
        long activeContacts = membershipService.countActiveByList(list.getId());

        if (!Objects.equals(list.getTotalContacts(), (int) totalContacts) || !Objects.equals(list.getActiveContacts(), (int) activeContacts)) {
            contactListService.updateStatistics(list.getId(), list.getUserId(), (int) totalContacts, (int) activeContacts);
        }

        return new ContactListSummaryView(
            list.getId(),
            list.getName(),
            blankToNull(list.getDescription()),
            (int) totalContacts,
            (int) activeContacts,
            Boolean.TRUE.equals(list.getDoubleOptInEnabled()),
            formatDate(list.getUpdatedAt())
        );
    }

    private ContactListDetailView toListDetailView(ContactList list) {
        ContactListSummaryView summary = toListSummaryView(list);
        return new ContactListDetailView(
            summary.id(),
            summary.name(),
            summary.description(),
            summary.totalContacts(),
            summary.activeContacts(),
            summary.doubleOptInEnabled(),
            formatDate(list.getCreatedAt()),
            summary.updatedAt()
        );
    }

    private ContactListForm toListForm(ContactList list) {
        ContactListForm form = new ContactListForm();
        form.name = list.getName();
        form.description = list.getDescription();
        form.doubleOptInEnabled = Boolean.TRUE.equals(list.getDoubleOptInEnabled());
        return form;
    }

    private ContactListMemberView toListMemberView(ContactListMembership membership) {
        Contact contact = membership.getContact();
        return new ContactListMemberView(
            contact.getId(),
            buildContactName(contact),
            contact.getEmail(),
            contact.getStatus(),
            formatDate(membership.getAddedAt() != null ? membership.getAddedAt() : membership.getCreatedAt())
        );
    }

    private long countContactsWithoutLists(String userId) {
        return contactService.findByUserId(userId).stream()
            .filter(contact -> membershipService.findByContact(contact.getId()).isEmpty())
            .count();
    }

    private void refreshListStatistics(String listId, String userId) {
        contactListService.updateStatistics(
            listId,
            userId,
            (int) membershipService.countByList(listId),
            (int) membershipService.countActiveByList(listId)
        );
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
    ) { }

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
    ) { }

    public static class ContactListForm {
        @NotBlank(message = "List name is required.")
        public String name;
        public String description;
        public Boolean doubleOptInEnabled = true;
    }

    private record ContactListSummaryView(
        String id,
        String name,
        String description,
        int totalContacts,
        int activeContacts,
        boolean doubleOptInEnabled,
        String updatedAt
    ) { }

    private record ContactListDetailView(
        String id,
        String name,
        String description,
        int totalContacts,
        int activeContacts,
        boolean doubleOptInEnabled,
        String createdAt,
        String updatedAt
    ) { }

    private record ContactListMemberView(
        String id,
        String name,
        String email,
        String status,
        String addedAt
    ) { }

    public static class ContactForm {
        private String id;
        private String firstName;
        private String lastName;
        @NotBlank(message = "Email address is required.")
        @Email(message = "Enter a valid email address.")
        private String email;
        private String company;
        private String phone;
        private String country;
        private String status = "SUBSCRIBED";
        private List<String> tags = Collections.emptyList();
        private List<String> selectedListIds = Collections.emptyList();
        private String notes;
        private boolean gdprConsent;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags != null ? tags : Collections.emptyList();
        }

        public List<String> getSelectedListIds() {
            return selectedListIds;
        }

        public void setSelectedListIds(List<String> selectedListIds) {
            this.selectedListIds = selectedListIds != null ? selectedListIds : Collections.emptyList();
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public boolean isGdprConsent() {
            return gdprConsent;
        }

        public void setGdprConsent(boolean gdprConsent) {
            this.gdprConsent = gdprConsent;
        }
    }
}
