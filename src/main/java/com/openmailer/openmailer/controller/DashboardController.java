package com.openmailer.openmailer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard - OpenMailer");

        // Stats data
        model.addAttribute("totalCampaigns", 24);
        model.addAttribute("totalEmailsSent", "145,678");
        model.addAttribute("avgOpenRate", "42.5%");
        model.addAttribute("avgClickRate", "12.3%");

        // Trends (for stat cards)
        model.addAttribute("campaignsTrend", "+12.5%");
        model.addAttribute("emailsTrend", "+8.2%");
        model.addAttribute("openRateTrend", "+5.1%");
        model.addAttribute("clickRateTrend", "-2.3%");

        // Chart data - Activity over last 7 days
        model.addAttribute("activityLabels", Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        model.addAttribute("activityData", Arrays.asList(1250, 1580, 1420, 1890, 2100, 1650, 1320));

        // Chart data - Top campaigns
        model.addAttribute("campaignLabels", Arrays.asList("Summer Sale", "Newsletter #42", "Welcome Series", "Black Friday"));
        model.addAttribute("campaignOpenRates", Arrays.asList(45, 38, 52, 48));

        // Chart data - Email status
        model.addAttribute("statusLabels", Arrays.asList("Delivered", "Opened", "Clicked", "Bounced"));
        model.addAttribute("statusData", Arrays.asList(12450, 5290, 1530, 180));

        // Recent campaigns
        List<Map<String, Object>> recentCampaigns = new ArrayList<>();

        Map<String, Object> campaign1 = new HashMap<>();
        campaign1.put("id", "1");
        campaign1.put("name", "Summer Sale 2024");
        campaign1.put("status", "SENT");
        campaign1.put("sent", "5,234");
        campaign1.put("opens", "2,156");
        campaign1.put("clicks", "543");
        campaign1.put("sentDate", "2024-12-20");
        recentCampaigns.add(campaign1);

        Map<String, Object> campaign2 = new HashMap<>();
        campaign2.put("id", "2");
        campaign2.put("name", "Newsletter #42");
        campaign2.put("status", "SENT");
        campaign2.put("sent", "8,901");
        campaign2.put("opens", "3,384");
        campaign2.put("clicks", "892");
        campaign2.put("sentDate", "2024-12-18");
        recentCampaigns.add(campaign2);

        Map<String, Object> campaign3 = new HashMap<>();
        campaign3.put("id", "3");
        campaign3.put("name", "Product Launch");
        campaign3.put("status", "SENDING");
        campaign3.put("sent", "2,100");
        campaign3.put("opens", "—");
        campaign3.put("clicks", "—");
        campaign3.put("sentDate", "In Progress");
        recentCampaigns.add(campaign3);

        Map<String, Object> campaign4 = new HashMap<>();
        campaign4.put("id", "4");
        campaign4.put("name", "Welcome Series");
        campaign4.put("status", "SCHEDULED");
        campaign4.put("sent", "—");
        campaign4.put("opens", "—");
        campaign4.put("clicks", "—");
        campaign4.put("sentDate", "2024-12-28");
        recentCampaigns.add(campaign4);

        Map<String, Object> campaign5 = new HashMap<>();
        campaign5.put("id", "5");
        campaign5.put("name", "Black Friday Deals");
        campaign5.put("status", "DRAFT");
        campaign5.put("sent", "—");
        campaign5.put("opens", "—");
        campaign5.put("clicks", "—");
        campaign5.put("sentDate", "Not scheduled");
        recentCampaigns.add(campaign5);

        model.addAttribute("recentCampaigns", recentCampaigns);

        return "dashboard";
    }
}
