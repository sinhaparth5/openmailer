package com.openmailer.openmailer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contacts")
public class ContactsController {

    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            Model model) {

        model.addAttribute("pageTitle", "Contacts - OpenMailer");

        // Stats summary
        model.addAttribute("totalContacts", 15842);
        model.addAttribute("subscribedContacts", 14523);
        model.addAttribute("unsubscribedContacts", 892);
        model.addAttribute("bouncedContacts", 427);

        // Filter options
        List<Map<String, String>> statusFilters = Arrays.asList(
            Map.of("value", "", "label", "All Statuses"),
            Map.of("value", "SUBSCRIBED", "label", "Subscribed"),
            Map.of("value", "UNSUBSCRIBED", "label", "Unsubscribed"),
            Map.of("value", "PENDING", "label", "Pending"),
            Map.of("value", "BOUNCED", "label", "Bounced")
        );
        model.addAttribute("statusFilters", statusFilters);

        List<Map<String, String>> tagFilters = Arrays.asList(
            Map.of("value", "", "label", "All Tags"),
            Map.of("value", "VIP", "label", "VIP"),
            Map.of("value", "Newsletter", "label", "Newsletter"),
            Map.of("value", "Customer", "label", "Customer"),
            Map.of("value", "Lead", "label", "Lead")
        );
        model.addAttribute("tagFilters", tagFilters);

        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("currentTag", tag != null ? tag : "");
        model.addAttribute("searchQuery", search != null ? search : "");

        // Contact list
        List<Map<String, Object>> contacts = createSampleContacts();

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            contacts = contacts.stream()
                .filter(c -> status.equals(c.get("status")))
                .collect(Collectors.toList());
        }

        // Filter by tag if provided
        if (tag != null && !tag.isEmpty()) {
            contacts = contacts.stream()
                .filter(c -> {
                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) c.get("tags");
                    return tags.contains(tag);
                })
                .collect(Collectors.toList());
        }

        // Filter by search if provided
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            contacts = contacts.stream()
                .filter(c ->
                    ((String)c.get("name")).toLowerCase().contains(searchLower) ||
                    ((String)c.get("email")).toLowerCase().contains(searchLower)
                )
                .collect(Collectors.toList());
        }

        model.addAttribute("contacts", contacts);

        // Pagination
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 5);
        model.addAttribute("pages", Arrays.asList(1, 2, 3, 4, 5));

        return "contacts/list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("pageTitle", "Add Contact - OpenMailer");
        model.addAttribute("mode", "add");
        return "contacts/form";
    }

    @GetMapping("/import")
    public String importContacts(Model model) {
        model.addAttribute("pageTitle", "Import Contacts - OpenMailer");

        // Import history
        List<Map<String, Object>> importHistory = new ArrayList<>();

        Map<String, Object> import1 = new HashMap<>();
        import1.put("id", "1");
        import1.put("fileName", "subscribers_2024_12.csv");
        import1.put("status", "COMPLETED");
        import1.put("total", 1234);
        import1.put("successful", 1180);
        import1.put("failed", 54);
        import1.put("date", "2024-12-20 14:30");
        importHistory.add(import1);

        Map<String, Object> import2 = new HashMap<>();
        import2.put("id", "2");
        import2.put("fileName", "newsletter_list.csv");
        import2.put("status", "COMPLETED");
        import2.put("total", 892);
        import2.put("successful", 892);
        import2.put("failed", 0);
        import2.put("date", "2024-12-18 10:15");
        importHistory.add(import2);

        Map<String, Object> import3 = new HashMap<>();
        import3.put("id", "3");
        import3.put("fileName", "customer_emails.xlsx");
        import3.put("status", "FAILED");
        import3.put("total", 0);
        import3.put("successful", 0);
        import3.put("failed", 0);
        import3.put("date", "2024-12-15 16:45");
        importHistory.add(import3);

        model.addAttribute("importHistory", importHistory);

        return "contacts/import";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Contact Details - OpenMailer");
        model.addAttribute("contact", getContactById(id));

        // Contact activity
        List<Map<String, Object>> activity = new ArrayList<>();

        Map<String, Object> activity1 = new HashMap<>();
        activity1.put("type", "OPENED");
        activity1.put("campaign", "Summer Sale 2024");
        activity1.put("date", "2024-12-20 09:15");
        activity1.put("icon", "mail-open");
        activity.add(activity1);

        Map<String, Object> activity2 = new HashMap<>();
        activity2.put("type", "CLICKED");
        activity2.put("campaign", "Newsletter #42");
        activity2.put("link", "https://example.com/product");
        activity2.put("date", "2024-12-18 14:42");
        activity2.put("icon", "cursor");
        activity.add(activity2);

        Map<String, Object> activity3 = new HashMap<>();
        activity3.put("type", "SUBSCRIBED");
        activity3.put("source", "Website Form");
        activity3.put("date", "2024-12-01 11:20");
        activity3.put("icon", "user-plus");
        activity.add(activity3);

        model.addAttribute("activity", activity);

        // Engagement stats
        model.addAttribute("totalEmailsReceived", 24);
        model.addAttribute("totalOpened", 18);
        model.addAttribute("totalClicked", 12);
        model.addAttribute("openRate", "75.0%");
        model.addAttribute("clickRate", "50.0%");

        return "contacts/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Edit Contact - OpenMailer");
        model.addAttribute("mode", "edit");
        model.addAttribute("contact", getContactById(id));
        return "contacts/form";
    }

    private List<Map<String, Object>> createSampleContacts() {
        List<Map<String, Object>> contacts = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            Map<String, Object> contact = new HashMap<>();
            contact.put("id", String.valueOf(i));
            contact.put("name", "Contact " + i);
            contact.put("email", "contact" + i + "@example.com");
            contact.put("status", i % 4 == 0 ? "UNSUBSCRIBED" : (i % 3 == 0 ? "PENDING" : "SUBSCRIBED"));
            contact.put("subscribedDate", "2024-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1));

            // Assign tags based on patterns
            List<String> tags = new ArrayList<>();
            if (i % 5 == 0) tags.add("VIP");
            if (i % 2 == 0) tags.add("Newsletter");
            if (i % 3 == 0) tags.add("Customer");
            if (i % 7 == 0) tags.add("Lead");
            if (tags.isEmpty()) tags.add("General");
            contact.put("tags", tags);

            contact.put("company", i % 3 == 0 ? "Company " + i : "");
            contact.put("phone", i % 2 == 0 ? "+1 555-" + String.format("%04d", i * 100) : "");
            contact.put("country", i % 3 == 0 ? "USA" : (i % 3 == 1 ? "UK" : "Canada"));

            contacts.add(contact);
        }

        return contacts;
    }

    private Map<String, Object> getContactById(String id) {
        return createSampleContacts().stream()
            .filter(c -> id.equals(c.get("id")))
            .findFirst()
            .orElse(createSampleContacts().get(0));
    }
}
