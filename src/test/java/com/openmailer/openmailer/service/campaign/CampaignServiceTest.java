package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.exception.ValidationException;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.Domain;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.model.EmailProvider;
import com.openmailer.openmailer.model.EmailTemplate;
import com.openmailer.openmailer.model.Segment;
import com.openmailer.openmailer.repository.EmailCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private EmailCampaignRepository campaignRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository);
        lenient().when(campaignRepository.save(any(EmailCampaign.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateCampaignPersistsAllEditableFieldsForDraftCampaign() {
        EmailCampaign existing = draftCampaign();
        existing.setId("campaign-1");
        existing.setUserId("user-1");

        EmailTemplate template = new EmailTemplate();
        ContactList list = new ContactList();
        Segment segment = new Segment();
        Domain domain = new Domain();
        EmailProvider provider = new EmailProvider();

        EmailCampaign updated = new EmailCampaign();
        updated.setName("Spring Launch");
        updated.setTemplate(template);
        updated.setContactList(list);
        updated.setSegment(segment);
        updated.setSubjectLine("Big announcement");
        updated.setPreviewText("Preview");
        updated.setFromName("Marketing");
        updated.setFromEmail("team@example.com");
        updated.setReplyToEmail("reply@example.com");
        updated.setDomain(domain);
        updated.setProvider(provider);
        updated.setTrackOpens(false);
        updated.setTrackClicks(false);
        updated.setTotalRecipients(450);

        when(campaignRepository.findByIdAndUserId("campaign-1", "user-1"))
            .thenReturn(Optional.of(existing));

        EmailCampaign result = campaignService.updateCampaign("campaign-1", "user-1", updated);

        assertEquals("Spring Launch", result.getName());
        assertSame(template, result.getTemplate());
        assertSame(list, result.getContactList());
        assertSame(segment, result.getSegment());
        assertEquals("Big announcement", result.getSubjectLine());
        assertEquals("Preview", result.getPreviewText());
        assertEquals("Marketing", result.getFromName());
        assertEquals("team@example.com", result.getFromEmail());
        assertEquals("reply@example.com", result.getReplyToEmail());
        assertSame(domain, result.getDomain());
        assertSame(provider, result.getProvider());
        assertEquals(Boolean.FALSE, result.getTrackOpens());
        assertEquals(Boolean.FALSE, result.getTrackClicks());
        assertEquals(450, result.getTotalRecipients());
    }

    @Test
    void scheduleCampaignRejectsNonDraftCampaigns() {
        EmailCampaign existing = draftCampaign();
        existing.setStatus("SENT");

        when(campaignRepository.findByIdAndUserId("campaign-1", "user-1"))
            .thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(
            ValidationException.class,
            () -> campaignService.scheduleCampaign("campaign-1", "user-1", LocalDateTime.now().plusHours(1))
        );

        assertEquals("status", ex.getField());
    }

    @Test
    void startSendingCampaignMarksCampaignAsSendingAndSetsSentAt() {
        EmailCampaign existing = draftCampaign();
        existing.setStatus("SCHEDULED");

        when(campaignRepository.findByIdAndUserId("campaign-1", "user-1"))
            .thenReturn(Optional.of(existing));

        EmailCampaign result = campaignService.startSendingCampaign("campaign-1", "user-1");

        assertEquals("SENDING", result.getStatus());
        verify(campaignRepository).save(existing);
    }

    @Test
    void cancelScheduledCampaignMovesCampaignBackToDraft() {
        EmailCampaign existing = draftCampaign();
        existing.setStatus("SCHEDULED");
        existing.setScheduledAt(LocalDateTime.now().plusDays(1));

        when(campaignRepository.findByIdAndUserId("campaign-1", "user-1"))
            .thenReturn(Optional.of(existing));

        EmailCampaign result = campaignService.cancelScheduledCampaign("campaign-1", "user-1");

        assertEquals("DRAFT", result.getStatus());
        assertNull(result.getScheduledAt());
    }

    private EmailCampaign draftCampaign() {
        EmailCampaign campaign = new EmailCampaign();
        campaign.setStatus("DRAFT");
        return campaign;
    }
}
