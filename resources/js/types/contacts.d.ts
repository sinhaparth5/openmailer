export type ContactStatus = 'subscribed' | 'unsubscribed' | 'bounced' | 'complained';

export interface ContactList {
    id: string;
    name: string;
    description?: string;
    total_contacts: number;
    created_at: string;
    updated_at: string;
}

export interface Contact {
    id: string;
    user_id: string;
    email: string;
    first_name: string | null;
    last_name: string | null;
    custom_fields: Record<string, string> | null;
    status: ContactStatus;
    bounce_type: 'none' | 'soft' | 'hard';
    subscribed_at: string | null;
    unsubscribed_at: string | null;
    created_at: string;
    updated_at: string;
    contact_lists?: ContactList[];
}

export interface ContactFormData {
    email: string;
    first_name: string;
    last_name: string;
    status?: ContactStatus;
    contact_list_ids: string[];
    custom_fields: Record<string, string>;
}

// Pagination Types
export interface PaginationLink {
    url: string | null;
    label: string;
    active: boolean;
}

export interface PaginatedResponse<T> {
    data: T[];
    links: PaginationLink[];
    prev_page_url: string | null;
    next_page_url: string | null;
    first_page_url: string;
    last_page_url: string;
    from: number;
    to: number;
    total: number;
    current_page: number;
    last_page: number;
    per_page: number;
}

export type PaginatedContacts = PaginatedResponse<Contact>;
export type PaginatedContactLists = PaginatedResponse<ContactList>;

// Filter Types
export interface ContactFilters {
    search?: string;
    status?: ContactStatus | '';
    list_id?: string;
}

// Inertia Page Props
export interface User {
    id: string;
    name: string;
    email: string;
    email_verified_at: string | null;
    created_at: string;
    updated_at: string;
}

export interface Auth {
    user: User;
}

export interface ContactIndexProps {
    auth: Auth;
    contacts: PaginatedContacts;
    filters: ContactFilters;
}

export interface ContactCreateProps {
    auth: Auth;
    contactLists: ContactList[];
}

export interface ContactEditProps {
    auth: Auth;
    contact: Contact;
    contactLists: ContactList[];
}

// API Response Types
export interface ApiResponse<T = any> {
    message: string;
    data?: T;
}

export interface ApiErrorResponse {
    message: string;
    errors?: Record<string, string[]>;
}

export interface ImportResult {
    imported: number;
    skipped: number;
    errors: string[];
}

export interface ImportResponse extends ApiResponse {
    result: ImportResult;
}
