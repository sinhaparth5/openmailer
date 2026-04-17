package com.openmailer.openmailer.model;

import com.openmailer.openmailer.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "contact_list_memberships")
public class ContactListMembership {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne
    @JoinColumn(name = "list_id", nullable = false)
    private ContactList contactList;

    @Column(name = "contact_id", nullable = false, insertable = false, updatable = false, length = 50)
    private String contactId;

    @Column(name = "list_id", nullable = false, insertable = false, updatable = false, length = 50)
    private String listId;

    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ContactListMembership() {
    }

    public ContactListMembership(Contact contact, ContactList contactList) {
        this.contact = contact;
        this.contactList = contactList;
        this.status = "ACTIVE";
        this.subscribedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generateId();
        }
        createdAt = LocalDateTime.now();
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
        this.contactId = contact != null ? contact.getId() : null;
    }

    public ContactList getContactList() {
        return contactList;
    }

    public void setContactList(ContactList contactList) {
        this.contactList = contactList;
        this.listId = contactList != null ? contactList.getId() : null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }

    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
        if (contactId == null) {
            this.contact = null;
            return;
        }
        if (this.contact == null || !Objects.equals(this.contact.getId(), contactId)) {
            Contact contact = new Contact();
            contact.setId(contactId);
            this.contact = contact;
        }
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
        if (listId == null) {
            this.contactList = null;
            return;
        }
        if (this.contactList == null || !Objects.equals(this.contactList.getId(), listId)) {
            ContactList contactList = new ContactList();
            contactList.setId(listId);
            this.contactList = contactList;
        }
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
