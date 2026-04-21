package com.openmailer.openmailer.service.campaign;

import com.openmailer.openmailer.model.Contact;
import com.openmailer.openmailer.model.ContactList;
import com.openmailer.openmailer.model.EmailCampaign;
import com.openmailer.openmailer.model.Segment;
import com.openmailer.openmailer.repository.ContactListRepository;
import com.openmailer.openmailer.repository.ContactRepository;
import com.openmailer.openmailer.service.contact.ContactListMembershipService;
import com.openmailer.openmailer.service.contact.SegmentEvaluationService;
import com.openmailer.openmailer.service.contact.SegmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CampaignAudienceService {

    private final ContactListRepository contactListRepository;
    private final ContactListMembershipService membershipService;
    private final ContactRepository contactRepository;
    private final SegmentService segmentService;
    private final SegmentEvaluationService segmentEvaluationService;

    public CampaignAudienceService(
        ContactListRepository contactListRepository,
        ContactListMembershipService membershipService,
        ContactRepository contactRepository,
        SegmentService segmentService,
        SegmentEvaluationService segmentEvaluationService
    ) {
        this.contactListRepository = contactListRepository;
        this.membershipService = membershipService;
        this.contactRepository = contactRepository;
        this.segmentService = segmentService;
        this.segmentEvaluationService = segmentEvaluationService;
    }

    public AudiencePreflight evaluate(String userId, String listId, String segmentId) {
        ContactList list = null;
        if (listId != null && !listId.isBlank()) {
            list = contactListRepository.findByIdAndUser_Id(listId, userId).orElse(null);
        }

        Segment segment = null;
        if (segmentId != null && !segmentId.isBlank()) {
            segment = segmentService.findByIdAndUserId(segmentId, userId);
        }

        return evaluate(list, segment);
    }

    public AudiencePreflight evaluate(EmailCampaign campaign) {
        return evaluate(campaign.getContactList(), campaign.getSegment());
    }

    public List<Contact> resolveReachableContacts(String userId, String listId, String segmentId) {
        ContactList list = null;
        if (listId != null && !listId.isBlank()) {
            list = contactListRepository.findByIdAndUser_Id(listId, userId).orElse(null);
        }

        Segment segment = null;
        if (segmentId != null && !segmentId.isBlank()) {
            segment = segmentService.findByIdAndUserId(segmentId, userId);
        }

        return resolveReachableContacts(list, segment);
    }

    public List<Contact> resolveReachableContacts(EmailCampaign campaign) {
        return resolveReachableContacts(campaign.getContactList(), campaign.getSegment());
    }

    private AudiencePreflight evaluate(ContactList list, Segment segment) {
        if (list == null) {
            return AudiencePreflight.empty(segment != null, segment != null ? segment.getCachedCount() : null);
        }

        List<String> activeMembershipContactIds = membershipService.getActiveContactIdsByList(list.getId());
        List<Contact> activeContacts = loadContacts(activeMembershipContactIds);
        List<Contact> subscribedContacts = activeContacts.stream()
            .filter(contact -> "SUBSCRIBED".equals(contact.getStatus()))
            .toList();
        if (segment != null && segment.getContactList() != null && !segment.getContactList().getId().equals(list.getId())) {
            return new AudiencePreflight(
                list.getId(),
                list.getName(),
                safeInt(list.getTotalContacts()),
                activeMembershipContactIds.size(),
                0,
                activeContacts.size(),
                segment.getCachedCount(),
                true,
                true,
                segmentWarning(segment, list)
            );
        }
        List<Contact> filteredContacts = segment != null
            ? segmentEvaluationService.filterContacts(subscribedContacts, segment)
            : subscribedContacts;
        int suppressedContacts = activeContacts.size() - filteredContacts.size();
        Integer estimatedSegmentSize = segment != null ? filteredContacts.size() : null;

        return new AudiencePreflight(
            list.getId(),
            list.getName(),
            safeInt(list.getTotalContacts()),
            activeMembershipContactIds.size(),
            filteredContacts.size(),
            suppressedContacts,
            estimatedSegmentSize,
            segment != null,
            filteredContacts.isEmpty(),
            segmentWarning(segment, list)
        );
    }

    private List<Contact> resolveReachableContacts(ContactList list, Segment segment) {
        if (list == null) {
            return List.of();
        }
        if (segment != null && segment.getContactList() != null && !segment.getContactList().getId().equals(list.getId())) {
            return List.of();
        }

        List<Contact> subscribedContacts = loadContacts(membershipService.getActiveContactIdsByList(list.getId())).stream()
            .filter(contact -> "SUBSCRIBED".equals(contact.getStatus()))
            .toList();

        if (segment == null) {
            return subscribedContacts;
        }
        return segmentEvaluationService.filterContacts(subscribedContacts, segment);
    }

    private List<Contact> loadContacts(List<String> contactIds) {
        return contactIds.isEmpty() ? List.of() : contactRepository.findAllById(contactIds);
    }

    private String segmentWarning(Segment segment, ContactList list) {
        if (segment == null) {
            return null;
        }
        if (segment.getContactList() != null && list != null && !segment.getContactList().getId().equals(list.getId())) {
            return "This segment belongs to a different list. Select the same list on the campaign or remove the segment.";
        }
        return null;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    public static class AudiencePreflight {
        private final String listId;
        private final String listName;
        private final int totalListContacts;
        private final int activeMemberships;
        private final int reachableRecipients;
        private final int suppressedRecipients;
        private final Integer estimatedSegmentRecipients;
        private final boolean segmentSelected;
        private final boolean zeroAudience;
        private final String warning;

        public AudiencePreflight(
            String listId,
            String listName,
            int totalListContacts,
            int activeMemberships,
            int reachableRecipients,
            int suppressedRecipients,
            Integer estimatedSegmentRecipients,
            boolean segmentSelected,
            boolean zeroAudience,
            String warning
        ) {
            this.listId = listId;
            this.listName = listName;
            this.totalListContacts = totalListContacts;
            this.activeMemberships = activeMemberships;
            this.reachableRecipients = reachableRecipients;
            this.suppressedRecipients = suppressedRecipients;
            this.estimatedSegmentRecipients = estimatedSegmentRecipients;
            this.segmentSelected = segmentSelected;
            this.zeroAudience = zeroAudience;
            this.warning = warning;
        }

        public static AudiencePreflight empty(boolean segmentSelected, Integer estimatedSegmentRecipients) {
            return new AudiencePreflight(
                null,
                null,
                0,
                0,
                0,
                0,
                estimatedSegmentRecipients,
                segmentSelected,
                true,
                null
            );
        }

        public String getListId() {
            return listId;
        }

        public String getListName() {
            return listName;
        }

        public int getTotalListContacts() {
            return totalListContacts;
        }

        public int getActiveMemberships() {
            return activeMemberships;
        }

        public int getReachableRecipients() {
            return reachableRecipients;
        }

        public int getSuppressedRecipients() {
            return suppressedRecipients;
        }

        public Integer getEstimatedSegmentRecipients() {
            return estimatedSegmentRecipients;
        }

        public boolean isSegmentSelected() {
            return segmentSelected;
        }

        public boolean isZeroAudience() {
            return zeroAudience;
        }

        public String getWarning() {
            return warning;
        }
    }
}
