package com.openmailer.openmailer.controller;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.model.User;
import com.openmailer.openmailer.repository.ContactListRepository;
import com.openmailer.openmailer.repository.DomainRepository;
import com.openmailer.openmailer.repository.EmailProviderRepository;
import com.openmailer.openmailer.repository.EmailTemplateRepository;
import com.openmailer.openmailer.repository.SegmentRepository;
import com.openmailer.openmailer.security.CustomUserDetails;
import com.openmailer.openmailer.service.campaign.CampaignAudienceService;
import com.openmailer.openmailer.service.campaign.CampaignSendingService;
import com.openmailer.openmailer.service.campaign.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CampaignsControllerTest {

    @Mock
    private CampaignService campaignService;

    @Mock
    private CampaignAudienceService audienceService;

    @Mock
    private CampaignSendingService campaignSendingService;

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private ContactListRepository listRepository;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private EmailProviderRepository providerRepository;

    private CampaignsController controller;

    @BeforeEach
    void setUp() {
        controller = new CampaignsController(
            campaignService,
            audienceService,
            campaignSendingService,
            templateRepository,
            listRepository,
            segmentRepository,
            domainRepository,
            providerRepository
        );

        EmailCampaign campaign = new EmailCampaign();
        campaign.setId("campaign-1");
        campaign.setUserId("user-1");

        CampaignAudienceService.AudiencePreflight audiencePreflight =
            new CampaignAudienceService.AudiencePreflight(
                "list-1",
                "Main List",
                10,
                10,
                5,
                5,
                null,
                false,
                false,
                null
            );

        lenient().when(campaignService.findByIdAndUserId("campaign-1", "user-1")).thenReturn(campaign);
        lenient().when(audienceService.evaluate(campaign)).thenReturn(audiencePreflight);
    }

    @Test
    void sendCampaignStartsAsyncSendAndSetsSuccessFlash() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.sendCampaign("campaign-1", principal(), redirect);

        assertEquals("redirect:/campaigns/campaign-1", view);
        assertEquals("Campaign send started successfully.", redirect.getFlashAttributes().get("successMessage"));
        verify(campaignService).startSendingCampaign("campaign-1", "user-1");
        verify(campaignSendingService).sendCampaignAsync("campaign-1");
    }

    @Test
    void scheduleCampaignWithBlankDateSetsErrorFlash() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.scheduleCampaign("campaign-1", principal(), "", redirect);

        assertEquals("redirect:/campaigns/campaign-1", view);
        assertEquals(
            "Choose a future date and time for scheduling.",
            redirect.getFlashAttributes().get("errorMessage")
        );
    }

    @Test
    void scheduleCampaignDelegatesParsedTimestamp() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String scheduledAt = "2026-05-01T10:15";

        String view = controller.scheduleCampaign("campaign-1", principal(), scheduledAt, redirect);

        assertEquals("redirect:/campaigns/campaign-1", view);
        assertEquals("Campaign scheduled successfully.", redirect.getFlashAttributes().get("successMessage"));
        verify(campaignService).scheduleCampaign("campaign-1", "user-1", LocalDateTime.parse(scheduledAt));
    }

    @Test
    void cancelCampaignValidationFailureSetsErrorFlash() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        doThrow(new ValidationException("Can only cancel scheduled campaigns", "status"))
            .when(campaignService).cancelScheduledCampaign(eq("campaign-1"), eq("user-1"));

        String view = controller.cancelCampaign("campaign-1", principal(), redirect);

        assertEquals("redirect:/campaigns/campaign-1", view);
        assertEquals(
            "Can only cancel scheduled campaigns",
            redirect.getFlashAttributes().get("errorMessage")
        );
    }

    private CustomUserDetails principal() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setPassword("secret");
        user.setEnabled(true);
        return new CustomUserDetails(user);
    }
}
