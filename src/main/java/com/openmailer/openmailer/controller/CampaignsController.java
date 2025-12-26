package com.openmailer.openmailer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("/campaigns")
public class CampaignsController {

    @GetMapping
    public String list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {

        model.addAttribute("pageTitle", "Campaigns - OpenMailer");

        // Stats summary
        model.addAttribute("totalCampaigns", 24);
        model.addAttribute("draftCampaigns", 5);
        model.addAttribute("scheduledCampaigns", 3);
        model.addAttribute("sentCampaigns", 16);

        // Filter options
        List<Map<String, String>> statusFilters = Arrays.asList(
            Map.of("value", "", "label", "All Statuses"),
            Map.of("value", "DRAFT", "label", "Draft"),
            Map.of("value", "SCHEDULED", "label", "Scheduled"),
            Map.of("value", "SENDING", "label", "Sending"),
            Map.of("value", "SENT", "label", "Sent"),
            Map.of("value", "PAUSED", "label", "Paused")
        );
        model.addAttribute("statusFilters", statusFilters);
        model.addAttribute("currentStatus", status != null ? status : "");
        model.addAttribute("searchQuery", search != null ? search : "");

        // Campaign list
        List<Map<String, Object>> campaigns = createSampleCampaigns();

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            campaigns.removeIf(c -> !status.equals(c.get("status")));
        }

        // Filter by search if provided
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            campaigns.removeIf(c -> !((String)c.get("name")).toLowerCase().contains(searchLower));
        }

        model.addAttribute("campaigns", campaigns);

        // Pagination
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 3);
        model.addAttribute("pages", Arrays.asList(1, 2, 3));

        return "campaigns/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("pageTitle", "Create Campaign - OpenMailer");
        model.addAttribute("mode", "create");
        return "campaigns/form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Campaign Details - OpenMailer");
        model.addAttribute("campaign", getCampaignById(id));

        // Campaign statistics
        model.addAttribute("totalSent", 12450);
        model.addAttribute("totalDelivered", 12268);
        model.addAttribute("totalOpened", 5290);
        model.addAttribute("totalClicked", 1530);
        model.addAttribute("totalBounced", 182);
        model.addAttribute("totalUnsubscribed", 23);

        model.addAttribute("deliveryRate", "98.5%");
        model.addAttribute("openRate", "43.1%");
        model.addAttribute("clickRate", "12.3%");
        model.addAttribute("bounceRate", "1.5%");

        // Activity over time
        model.addAttribute("activityLabels", Arrays.asList("12am", "4am", "8am", "12pm", "4pm", "8pm"));
        model.addAttribute("openData", Arrays.asList(120, 80, 450, 890, 1200, 650));
        model.addAttribute("clickData", Arrays.asList(30, 15, 85, 210, 380, 180));

        // Link performance
        List<Map<String, Object>> linkClicks = new ArrayList<>();
        linkClicks.add(Map.of("url", "https://example.com/product-1", "clicks", 542, "uniqueClicks", 389));
        linkClicks.add(Map.of("url", "https://example.com/special-offer", "clicks", 428, "uniqueClicks", 312));
        linkClicks.add(Map.of("url", "https://example.com/learn-more", "clicks", 315, "uniqueClicks", 245));
        linkClicks.add(Map.of("url", "https://example.com/contact", "clicks", 245, "uniqueClicks", 198));
        model.addAttribute("linkClicks", linkClicks);

        return "campaigns/view";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "Edit Campaign - OpenMailer");
        model.addAttribute("mode", "edit");
        model.addAttribute("campaign", getCampaignById(id));
        return "campaigns/form";
    }

    private List<Map<String, Object>> createSampleCampaigns() {
        List<Map<String, Object>> campaigns = new ArrayList<>();

        Map<String, Object> campaign1 = new HashMap<>();
        campaign1.put("id", "1");
        campaign1.put("name", "Summer Sale 2024");
        campaign1.put("status", "SENT");
        campaign1.put("subject", "Get 50% Off All Summer Items!");
        campaign1.put("recipients", 5234);
        campaign1.put("sent", 5234);
        campaign1.put("opens", 2156);
        campaign1.put("clicks", 543);
        campaign1.put("openRate", "41.2%");
        campaign1.put("clickRate", "10.4%");
        campaign1.put("sentDate", "2024-12-20 09:00");
        campaign1.put("createdDate", "2024-12-18");
        campaigns.add(campaign1);

        Map<String, Object> campaign2 = new HashMap<>();
        campaign2.put("id", "2");
        campaign2.put("name", "Newsletter #42 - December Edition");
        campaign2.put("status", "SENT");
        campaign2.put("subject", "Your Monthly Update is Here");
        campaign2.put("recipients", 8901);
        campaign2.put("sent", 8901);
        campaign2.put("opens", 3384);
        campaign2.put("clicks", 892);
        campaign2.put("openRate", "38.0%");
        campaign2.put("clickRate", "10.0%");
        campaign2.put("sentDate", "2024-12-18 14:30");
        campaign2.put("createdDate", "2024-12-15");
        campaigns.add(campaign2);

        Map<String, Object> campaign3 = new HashMap<>();
        campaign3.put("id", "3");
        campaign3.put("name", "Product Launch - New Collection");
        campaign3.put("status", "SENDING");
        campaign3.put("subject", "Introducing Our Latest Collection");
        campaign3.put("recipients", 4200);
        campaign3.put("sent", 2100);
        campaign3.put("opens", 0);
        campaign3.put("clicks", 0);
        campaign3.put("openRate", "—");
        campaign3.put("clickRate", "—");
        campaign3.put("sentDate", "In Progress");
        campaign3.put("createdDate", "2024-12-21");
        campaigns.add(campaign3);

        Map<String, Object> campaign4 = new HashMap<>();
        campaign4.put("id", "4");
        campaign4.put("name", "Welcome Series - Day 1");
        campaign4.put("status", "SCHEDULED");
        campaign4.put("subject", "Welcome to OpenMailer!");
        campaign4.put("recipients", 0);
        campaign4.put("sent", 0);
        campaign4.put("opens", 0);
        campaign4.put("clicks", 0);
        campaign4.put("openRate", "—");
        campaign4.put("clickRate", "—");
        campaign4.put("sentDate", "2024-12-28 10:00");
        campaign4.put("createdDate", "2024-12-19");
        campaigns.add(campaign4);

        Map<String, Object> campaign5 = new HashMap<>();
        campaign5.put("id", "5");
        campaign5.put("name", "Black Friday Deals Preview");
        campaign5.put("status", "DRAFT");
        campaign5.put("subject", "");
        campaign5.put("recipients", 0);
        campaign5.put("sent", 0);
        campaign5.put("opens", 0);
        campaign5.put("clicks", 0);
        campaign5.put("openRate", "—");
        campaign5.put("clickRate", "—");
        campaign5.put("sentDate", "Not scheduled");
        campaign5.put("createdDate", "2024-12-20");
        campaigns.add(campaign5);

        Map<String, Object> campaign6 = new HashMap<>();
        campaign6.put("id", "6");
        campaign6.put("name", "Customer Feedback Survey");
        campaign6.put("status", "SENT");
        campaign6.put("subject", "We'd Love Your Feedback!");
        campaign6.put("recipients", 3210);
        campaign6.put("sent", 3210);
        campaign6.put("opens", 1605);
        campaign6.put("clicks", 892);
        campaign6.put("openRate", "50.0%");
        campaign6.put("clickRate", "27.8%");
        campaign6.put("sentDate", "2024-12-15 11:00");
        campaign6.put("createdDate", "2024-12-13");
        campaigns.add(campaign6);

        Map<String, Object> campaign7 = new HashMap<>();
        campaign7.put("id", "7");
        campaign7.put("name", "Abandoned Cart Reminder");
        campaign7.put("status", "SENT");
        campaign7.put("subject", "Don't Forget Your Items!");
        campaign7.put("recipients", 1823);
        campaign7.put("sent", 1823);
        campaign7.put("opens", 892);
        campaign7.put("clicks", 456);
        campaign7.put("openRate", "48.9%");
        campaign7.put("clickRate", "25.0%");
        campaign7.put("sentDate", "2024-12-17 16:00");
        campaign7.put("createdDate", "2024-12-17");
        campaigns.add(campaign7);

        Map<String, Object> campaign8 = new HashMap<>();
        campaign8.put("id", "8");
        campaign8.put("name", "Holiday Gift Guide");
        campaign8.put("status", "SCHEDULED");
        campaign8.put("subject", "Perfect Gifts for Everyone");
        campaign8.put("recipients", 0);
        campaign8.put("sent", 0);
        campaign8.put("opens", 0);
        campaign8.put("clicks", 0);
        campaign8.put("openRate", "—");
        campaign8.put("clickRate", "—");
        campaign8.put("sentDate", "2024-12-24 08:00");
        campaign8.put("createdDate", "2024-12-20");
        campaigns.add(campaign8);

        Map<String, Object> campaign9 = new HashMap<>();
        campaign9.put("id", "9");
        campaign9.put("name", "Re-engagement Campaign");
        campaign9.put("status", "DRAFT");
        campaign9.put("subject", "");
        campaign9.put("recipients", 0);
        campaign9.put("sent", 0);
        campaign9.put("opens", 0);
        campaign9.put("clicks", 0);
        campaign9.put("openRate", "—");
        campaign9.put("clickRate", "—");
        campaign9.put("sentDate", "Not scheduled");
        campaign9.put("createdDate", "2024-12-21");
        campaigns.add(campaign9);

        Map<String, Object> campaign10 = new HashMap<>();
        campaign10.put("id", "10");
        campaign10.put("name", "VIP Customer Exclusive");
        campaign10.put("status", "SENT");
        campaign10.put("subject", "Exclusive Offer Just for You");
        campaign10.put("recipients", 892);
        campaign10.put("sent", 892);
        campaign10.put("opens", 623);
        campaign10.put("clicks", 245);
        campaign10.put("openRate", "69.8%");
        campaign10.put("clickRate", "27.5%");
        campaign10.put("sentDate", "2024-12-16 10:00");
        campaign10.put("createdDate", "2024-12-15");
        campaigns.add(campaign10);

        Map<String, Object> campaign11 = new HashMap<>();
        campaign11.put("id", "11");
        campaign11.put("name", "Weekly Digest - Week 51");
        campaign11.put("status", "PAUSED");
        campaign11.put("subject", "Your Week in Review");
        campaign11.put("recipients", 5600);
        campaign11.put("sent", 2800);
        campaign11.put("opens", 980);
        campaign11.put("clicks", 156);
        campaign11.put("openRate", "35.0%");
        campaign11.put("clickRate", "5.6%");
        campaign11.put("sentDate", "Paused");
        campaign11.put("createdDate", "2024-12-19");
        campaigns.add(campaign11);

        Map<String, Object> campaign12 = new HashMap<>();
        campaign12.put("id", "12");
        campaign12.put("name", "New Year Sale Announcement");
        campaign12.put("status", "DRAFT");
        campaign12.put("subject", "");
        campaign12.put("recipients", 0);
        campaign12.put("sent", 0);
        campaign12.put("opens", 0);
        campaign12.put("clicks", 0);
        campaign12.put("openRate", "—");
        campaign12.put("clickRate", "—");
        campaign12.put("sentDate", "Not scheduled");
        campaign12.put("createdDate", "2024-12-21");
        campaigns.add(campaign12);

        return campaigns;
    }

    private Map<String, Object> getCampaignById(String id) {
        return createSampleCampaigns().stream()
            .filter(c -> id.equals(c.get("id")))
            .findFirst()
            .orElse(createSampleCampaigns().get(0));
    }
}
