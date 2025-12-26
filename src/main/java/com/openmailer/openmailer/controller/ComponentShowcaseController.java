package com.openmailer.openmailer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
@RequestMapping("/components")
public class ComponentShowcaseController {

    @GetMapping
    public String componentsIndex(Model model) {
        model.addAttribute("pageTitle", "UI Components - OpenMailer");

        List<ComponentCategory> categories = Arrays.asList(
            new ComponentCategory("buttons", "Buttons", "Primary, secondary, outline, and more button variants", "🔘"),
            new ComponentCategory("cards", "Cards", "Stat cards, content cards, campaign cards, and more", "🃏"),
            new ComponentCategory("forms", "Forms", "Input fields, textareas, selects, toggles, and more", "📝"),
            new ComponentCategory("badges", "Badges", "Status badges, count badges, tags, and notifications", "🏷️"),
            new ComponentCategory("alerts", "Alerts & Toasts", "Success, error, warning, and info notifications", "⚠️"),
            new ComponentCategory("modals", "Modals", "Confirmation, info, form, and delete modals", "🪟"),
            new ComponentCategory("tables", "Tables", "Data tables with sorting, pagination, and selection", "📊"),
            new ComponentCategory("dropdowns", "Dropdowns", "Action menus, filters, and user dropdowns", "▼"),
            new ComponentCategory("charts", "Charts", "Line, bar, pie, and donut charts with Chart.js", "📈"),
            new ComponentCategory("loading", "Loading States", "Spinners, progress bars, and skeleton loaders", "⏳")
        );

        model.addAttribute("categories", categories);
        return "components-index";
    }

    @GetMapping("/{componentType}")
    public String showComponent(@PathVariable String componentType, Model model) {
        model.addAttribute("pageTitle", capitalize(componentType) + " Components - OpenMailer");
        model.addAttribute("componentType", componentType);

        // Add sample data based on component type
        switch (componentType) {
            case "buttons":
                addButtonsData(model);
                break;
            case "cards":
                addCardsData(model);
                break;
            case "forms":
                addFormsData(model);
                break;
            case "badges":
                addBadgesData(model);
                break;
            case "alerts":
                addAlertsData(model);
                break;
            case "modals":
                addModalsData(model);
                break;
            case "tables":
                addTablesData(model);
                break;
            case "dropdowns":
                addDropdownsData(model);
                break;
            case "charts":
                addChartsData(model);
                break;
            case "loading":
                addLoadingData(model);
                break;
            default:
                return "redirect:/components";
        }

        return "components-showcase";
    }

    private void addButtonsData(Model model) {
        model.addAttribute("title", "Button Components");
        model.addAttribute("description", "A collection of button variants for different actions and states.");
    }

    private void addCardsData(Model model) {
        model.addAttribute("title", "Card Components");
        model.addAttribute("description", "Various card layouts for displaying content, stats, and features.");

        // Sample campaign data
        Map<String, Object> campaign = new HashMap<>();
        campaign.put("name", "Summer Sale Campaign");
        campaign.put("subject", "Get 50% off on all products!");
        campaign.put("status", "SENT");
        campaign.put("sent", "1,234");
        campaign.put("opens", "456");
        campaign.put("clicks", "123");
        campaign.put("createdAt", "2024-01-15");
        model.addAttribute("sampleCampaign", campaign);

        // Sample contact data
        Map<String, Object> contact = new HashMap<>();
        contact.put("name", "John Doe");
        contact.put("email", "john@example.com");
        contact.put("initial", "JD");
        contact.put("status", "SUBSCRIBED");
        model.addAttribute("sampleContact", contact);
    }

    private void addFormsData(Model model) {
        model.addAttribute("title", "Form Components");
        model.addAttribute("description", "Input fields, selects, checkboxes, and other form elements.");

        // Sample options for select
        List<Map<String, String>> options = Arrays.asList(
            Map.of("value", "1", "label", "Option 1"),
            Map.of("value", "2", "label", "Option 2"),
            Map.of("value", "3", "label", "Option 3")
        );
        model.addAttribute("sampleOptions", options);
    }

    private void addBadgesData(Model model) {
        model.addAttribute("title", "Badge Components");
        model.addAttribute("description", "Status indicators, counts, and labels.");

        List<String> statuses = Arrays.asList("DRAFT", "SCHEDULED", "SENDING", "SENT", "PAUSED", "FAILED");
        model.addAttribute("statuses", statuses);
    }

    private void addAlertsData(Model model) {
        model.addAttribute("title", "Alert & Toast Components");
        model.addAttribute("description", "Notification alerts and toast messages for user feedback.");
    }

    private void addModalsData(Model model) {
        model.addAttribute("title", "Modal Components");
        model.addAttribute("description", "Dialog windows for confirmations, forms, and information.");
    }

    private void addTablesData(Model model) {
        model.addAttribute("title", "Table Components");
        model.addAttribute("description", "Data tables with sorting, filtering, and pagination.");

        // Sample table data
        List<Map<String, String>> contacts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, String> contact = new HashMap<>();
            contact.put("id", String.valueOf(i));
            contact.put("name", "Contact " + i);
            contact.put("email", "contact" + i + "@example.com");
            contact.put("status", i % 2 == 0 ? "SUBSCRIBED" : "PENDING");
            contact.put("createdAt", "2024-01-" + (10 + i));
            contact.put("initial", "C" + i);
            contact.put("tags", "Tag" + i);
            contacts.add(contact);
        }
        model.addAttribute("sampleContacts", contacts);
    }

    private void addDropdownsData(Model model) {
        model.addAttribute("title", "Dropdown Components");
        model.addAttribute("description", "Menu dropdowns for actions, filters, and navigation.");

        // Sample dropdown items
        List<Map<String, String>> items = Arrays.asList(
            Map.of("label", "Edit", "url", "#", "icon", "edit"),
            Map.of("label", "Duplicate", "url", "#", "icon", "duplicate"),
            Map.of("label", "Delete", "url", "#", "icon", "delete")
        );
        model.addAttribute("sampleItems", items);
    }

    private void addChartsData(Model model) {
        model.addAttribute("title", "Chart Components");
        model.addAttribute("description", "Data visualization with Chart.js integration.");
    }

    private void addLoadingData(Model model) {
        model.addAttribute("title", "Loading State Components");
        model.addAttribute("description", "Spinners, progress bars, and skeleton loaders.");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Inner class for component categories
    public static class ComponentCategory {
        private String id;
        private String name;
        private String description;
        private String icon;

        public ComponentCategory(String id, String name, String description, String icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
    }
}
