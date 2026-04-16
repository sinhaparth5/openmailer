package com.openmailer.openmailer.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactsControllerTest {

    @Mock
    private ContactService contactService;

    @Mock
    private ContactImportService contactImportService;

    @Mock
    private ContactListService contactListService;

    @Mock
    private ContactListMembershipService membershipService;

    @Mock
    private ContactListRepository contactListRepository;

    private ContactsController controller;

    @BeforeEach
    void setUp() {
        controller = new ContactsController(
            contactService,
            contactImportService,
            contactListService,
            membershipService,
            contactListRepository
        );
    }

    @Test
    void createAssignsSelectedListsToNewContact() {
        User user = user();
        CustomUserDetails principal = new CustomUserDetails(user);
        ContactsController.ContactForm form = new ContactsController.ContactForm();
        form.setEmail("person@example.com");
        form.setSelectedListIds(List.of("list-1", "list-2"));

        Contact saved = new Contact();
        saved.setId("contact-1");
        saved.setUser(user);
        saved.setEmail("person@example.com");

        when(contactService.createContact(any(Contact.class))).thenReturn(saved);
        when(contactListRepository.findByIdAndUser_Id(eq("list-1"), eq("user-1")))
            .thenReturn(Optional.of(list("list-1")));
        when(contactListRepository.findByIdAndUser_Id(eq("list-2"), eq("user-1")))
            .thenReturn(Optional.of(list("list-2")));
        when(membershipService.isContactInList("contact-1", "list-1")).thenReturn(false);
        when(membershipService.isContactInList("contact-1", "list-2")).thenReturn(false);
        when(membershipService.findByContact("contact-1")).thenReturn(List.of());

        String view = controller.create(
            principal,
            form,
            new BeanPropertyBindingResult(form, "contactForm"),
            new org.springframework.ui.ExtendedModelMap(),
            new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/contacts/contact-1", view);

        ArgumentCaptor<ContactListMembership> captor = ArgumentCaptor.forClass(ContactListMembership.class);
        verify(membershipService, times(2)).addContactToList(captor.capture());
        List<ContactListMembership> createdMemberships = captor.getAllValues();
        assertEquals(List.of("list-1", "list-2"),
            createdMemberships.stream().map(ContactListMembership::getListId).toList());
    }

    @Test
    void updateRemovesDeselectedListsAndAddsNewOnes() {
        User user = user();
        CustomUserDetails principal = new CustomUserDetails(user);
        ContactsController.ContactForm form = new ContactsController.ContactForm();
        form.setEmail("person@example.com");
        form.setSelectedListIds(List.of("list-2"));

        ContactListMembership oldMembership = new ContactListMembership();
        oldMembership.setContactId("contact-1");
        oldMembership.setListId("list-1");

        when(contactListRepository.findByIdAndUser_Id(eq("list-2"), eq("user-1")))
            .thenReturn(Optional.of(list("list-2")));
        when(membershipService.isContactInList("contact-1", "list-2")).thenReturn(false);
        when(membershipService.findByContact("contact-1")).thenReturn(List.of(oldMembership));

        String view = controller.update(
            "contact-1",
            principal,
            form,
            new BeanPropertyBindingResult(form, "contactForm"),
            new org.springframework.ui.ExtendedModelMap(),
            new RedirectAttributesModelMap()
        );

        assertEquals("redirect:/contacts/contact-1", view);
        verify(membershipService).removeContactFromList("contact-1", "list-1");
        verify(membershipService).addContactToList(any(ContactListMembership.class));
    }

    @Test
    void createReturnsFormWhenBindingFails() {
        User user = user();
        CustomUserDetails principal = new CustomUserDetails(user);
        ContactsController.ContactForm form = new ContactsController.ContactForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "contactForm");
        bindingResult.rejectValue("email", "required", "Email is required.");

        String view = controller.create(
            principal,
            form,
            bindingResult,
            new org.springframework.ui.ExtendedModelMap(),
            new RedirectAttributesModelMap()
        );

        assertEquals("contacts/form", view);
        verify(contactService, never()).createContact(any(Contact.class));
    }

    private User user() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("owner@example.com");
        user.setPassword("secret");
        user.setEnabled(true);
        return user;
    }

    private ContactList list(String id) {
        ContactList list = new ContactList();
        list.setId(id);
        list.setUser(user());
        list.setName("List " + id);
        return list;
    }
}
