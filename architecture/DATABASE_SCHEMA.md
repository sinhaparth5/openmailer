# OpenMailer - Database Schema

## Table of Contents
1. [Overview](#overview)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [Existing Tables (To Update)](#existing-tables-to-update)
4. [New Tables](#new-tables)
5. [Indexes Strategy](#indexes-strategy)
6. [Data Retention Policies](#data-retention-policies)

---

## Overview

OpenMailer uses **PostgreSQL** as its primary database with the following characteristics:

- **Total Tables:** 17 tables
- **Existing:** 3 tables (users, email_templates, email_campaigns)
- **New:** 14 additional tables
- **Migration Tool:** Liquibase for version control
- **Key Features:**
  - JSONB for flexible custom fields
  - Array types for tags
  - Comprehensive indexing strategy
  - Foreign key constraints for data integrity
  - Audit timestamps (created_at, updated_at)

---

## Entity Relationship Diagram

```
┌─────────────┐
│    users    │──────┐
└─────────────┘      │
      │              │
      │ creates      │ owns
      │              │
      ▼              ▼
┌──────────────┐  ┌────────────┐       ┌─────────────────┐
│email_templates│  │  domains   │───────│email_providers  │
└──────────────┘  └────────────┘       └─────────────────┘
      │                  │                       │
      │                  │ verified_for          │ uses
      │                  │                       │
      │                  ▼                       │
      │          ┌──────────────────┐           │
      └─────────▶│email_campaigns   │◀──────────┘
                 └──────────────────┘
                          │
                          │ has
                          │
                          ▼
                ┌──────────────────────┐
                │campaign_recipients   │──────┐
                └──────────────────────┘      │
                          │                    │
                          │                    │ tracks
                          ▼                    │
         ┌─────────────────────────┐          │
         │     contacts            │◀─────────┘
         └─────────────────────────┘
                   │      │
         ┌─────────┘      └──────────┐
         │ member_of                  │ filtered_by
         │                            │
         ▼                            ▼
┌──────────────────┐          ┌─────────────┐
│contact_lists     │          │  segments   │
└──────────────────┘          └─────────────┘
         │
         │ membership
         │
         ▼
┌───────────────────────────┐
│contact_list_memberships   │
└───────────────────────────┘


┌──────────────────┐      ┌──────────────────┐
│campaign_links    │──────│campaign_clicks   │
└──────────────────┘      └──────────────────┘


┌──────────────┐   ┌───────────────┐   ┌────────────────┐
│  api_keys    │   │ rate_limits   │   │  email_logs    │
└──────────────┘   └───────────────┘   └────────────────┘


┌──────────────────┐
│webhook_events    │
└──────────────────┘
```

---

## Existing Tables (To Update)

### 1. users

**Purpose:** User accounts and authentication

**Current Columns:**
- `id` BIGSERIAL PRIMARY KEY
- `username` VARCHAR(255) UNIQUE NOT NULL
- `email` VARCHAR(255) UNIQUE NOT NULL
- `password` VARCHAR(255) NOT NULL (BCrypt hashed)
- `enabled` BOOLEAN DEFAULT TRUE NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

**New Columns to Add:**
```sql
ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'USER' NOT NULL;
ALTER TABLE users ADD COLUMN account_status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE users ADD COLUMN subscription_plan VARCHAR(50) DEFAULT 'FREE';
ALTER TABLE users ADD COLUMN subscription_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN api_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN monthly_email_limit INT DEFAULT 1000;
ALTER TABLE users ADD COLUMN monthly_emails_sent INT DEFAULT 0;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN last_login_ip VARCHAR(45);
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(255);
```

**Indexes:**
- `idx_users_username` ON (username)
- `idx_users_email` ON (email)
- `idx_users_role` ON (role)

**Enums:**
- `role`: USER, ADMIN
- `account_status`: ACTIVE, SUSPENDED, TRIAL, EXPIRED
- `subscription_plan`: FREE, BASIC, PRO, ENTERPRISE

---

### 2. email_templates

**Purpose:** Reusable email templates with WYSIWYG/HTML content

**Current Columns:**
- `id` BIGSERIAL PRIMARY KEY
- `name` VARCHAR(255) UNIQUE NOT NULL
- `subject` VARCHAR(500) NOT NULL
- `body` TEXT NOT NULL
- `description` TEXT
- `created_by` BIGINT REFERENCES users(id) NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

**New Columns to Add:**
```sql
ALTER TABLE email_templates ADD COLUMN template_type VARCHAR(50) DEFAULT 'CUSTOM';
ALTER TABLE email_templates ADD COLUMN is_html BOOLEAN DEFAULT TRUE;
ALTER TABLE email_templates ADD COLUMN version INT DEFAULT 1;
ALTER TABLE email_templates ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE email_templates ADD COLUMN preview_text VARCHAR(255);
```

**Indexes:**
- `idx_email_templates_name` ON (name)
- `idx_email_templates_created_by` ON (created_by)
- `idx_email_templates_type` ON (template_type)

**Template Variables:**
Templates support variables like `{{first_name}}`, `{{last_name}}`, `{{custom.field_name}}`

---

### 3. email_campaigns

**Purpose:** Email marketing campaigns

**Current Columns:**
- `id` BIGSERIAL PRIMARY KEY
- `name` VARCHAR(255) NOT NULL
- `template_id` BIGINT REFERENCES email_templates(id) NOT NULL
- `status` VARCHAR(50) DEFAULT 'DRAFT' NOT NULL
- `scheduled_at` TIMESTAMP
- `sent_at` TIMESTAMP
- `total_recipients` INT DEFAULT 0
- `sent_count` INT DEFAULT 0
- `failed_count` INT DEFAULT 0
- `created_by` BIGINT REFERENCES users(id) NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

**New Columns to Add:**
```sql
ALTER TABLE email_campaigns ADD COLUMN from_name VARCHAR(255);
ALTER TABLE email_campaigns ADD COLUMN from_email VARCHAR(255);
ALTER TABLE email_campaigns ADD COLUMN reply_to_email VARCHAR(255);
ALTER TABLE email_campaigns ADD COLUMN domain_id BIGINT REFERENCES domains(id);
ALTER TABLE email_campaigns ADD COLUMN provider_id BIGINT REFERENCES email_providers(id);
ALTER TABLE email_campaigns ADD COLUMN list_id BIGINT REFERENCES contact_lists(id);
ALTER TABLE email_campaigns ADD COLUMN segment_id BIGINT REFERENCES segments(id);
ALTER TABLE email_campaigns ADD COLUMN subject_line VARCHAR(500);
ALTER TABLE email_campaigns ADD COLUMN preview_text VARCHAR(255);
ALTER TABLE email_campaigns ADD COLUMN track_opens BOOLEAN DEFAULT TRUE;
ALTER TABLE email_campaigns ADD COLUMN track_clicks BOOLEAN DEFAULT TRUE;
ALTER TABLE email_campaigns ADD COLUMN open_rate DECIMAL(5,2) DEFAULT 0;
ALTER TABLE email_campaigns ADD COLUMN click_rate DECIMAL(5,2) DEFAULT 0;
ALTER TABLE email_campaigns ADD COLUMN bounce_rate DECIMAL(5,2) DEFAULT 0;
ALTER TABLE email_campaigns ADD COLUMN unsubscribe_count INT DEFAULT 0;
ALTER TABLE email_campaigns ADD COLUMN complaint_count INT DEFAULT 0;
ALTER TABLE email_campaigns ADD COLUMN send_speed INT DEFAULT 100;
ALTER TABLE email_campaigns ADD COLUMN retry_failed BOOLEAN DEFAULT TRUE;
ALTER TABLE email_campaigns ADD COLUMN max_retries INT DEFAULT 3;
```

**Indexes:**
- `idx_email_campaigns_status` ON (status)
- `idx_email_campaigns_created_by` ON (created_by)
- `idx_email_campaigns_scheduled_at` ON (scheduled_at)

**Status Enum:**
DRAFT, SCHEDULED, SENDING, SENT, PAUSED, CANCELLED, FAILED

---

## New Tables

### 4. domains

**Purpose:** Custom domains for email sending with DNS verification

```sql
CREATE TABLE domains (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- SPF Record
    spf_record TEXT,
    spf_verified BOOLEAN DEFAULT FALSE,
    spf_last_checked_at TIMESTAMP,

    -- DKIM Record
    dkim_selector VARCHAR(100) DEFAULT 'openmailer',
    dkim_public_key TEXT,
    dkim_private_key TEXT,  -- AES-256 encrypted
    dkim_verified BOOLEAN DEFAULT FALSE,
    dkim_last_checked_at TIMESTAMP,

    -- DMARC Record
    dmarc_record TEXT,
    dmarc_verified BOOLEAN DEFAULT FALSE,
    dmarc_last_checked_at TIMESTAMP,

    -- Verification
    verification_token VARCHAR(255) UNIQUE,
    verified_at TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT idx_domains_user_id_unique UNIQUE (user_id, domain_name)
);

CREATE INDEX idx_domains_user_id ON domains(user_id);
CREATE INDEX idx_domains_status ON domains(status);
CREATE INDEX idx_domains_domain_name ON domains(domain_name);
```

**Status Enum:** PENDING, VERIFIED, FAILED, SUSPENDED

**Notes:**
- DKIM keys generated per domain (2048-bit RSA)
- Private keys encrypted before storage
- Verification runs via cron job every hour

---

### 5. email_providers

**Purpose:** Email sending service configurations (AWS SES, SendGrid, SMTP)

```sql
CREATE TABLE email_providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain_id BIGINT REFERENCES domains(id) ON DELETE SET NULL,

    provider_type VARCHAR(50) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,

    -- Encrypted configuration JSON
    configuration TEXT NOT NULL,  -- Encrypted credentials/API keys

    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    priority INT DEFAULT 0,  -- For failover (lower = higher priority)

    -- Limits
    daily_limit INT,
    monthly_limit INT,

    -- Status tracking
    status VARCHAR(50) DEFAULT 'ACTIVE',
    last_error TEXT,
    last_error_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_providers_user_id ON email_providers(user_id);
CREATE INDEX idx_email_providers_active ON email_providers(is_active, is_default);
CREATE UNIQUE INDEX idx_user_default_provider ON email_providers(user_id, is_default)
    WHERE is_default = TRUE;
```

**Provider Types:** AWS_SES, SENDGRID, SMTP

**Configuration JSON Structure:**
```json
{
  "aws_ses": {
    "region": "us-east-1",
    "access_key_id": "encrypted",
    "secret_access_key": "encrypted"
  },
  "sendgrid": {
    "api_key": "encrypted"
  },
  "smtp": {
    "host": "smtp.example.com",
    "port": 587,
    "username": "encrypted",
    "password": "encrypted",
    "use_tls": true
  }
}
```

---

### 6. contact_lists

**Purpose:** Organized collections of contacts

```sql
CREATE TABLE contact_lists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Statistics (cached)
    total_contacts INT DEFAULT 0,
    active_contacts INT DEFAULT 0,  -- Status = SUBSCRIBED

    -- Settings
    double_opt_in_enabled BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_list_name_per_user UNIQUE (user_id, name)
);

CREATE INDEX idx_contact_lists_user_id ON contact_lists(user_id);
```

---

### 7. contacts

**Purpose:** Individual contact records with custom fields

```sql
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Basic info
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),

    -- Status tracking
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN DEFAULT FALSE,

    -- Double opt-in
    confirmation_token VARCHAR(255) UNIQUE,
    confirmation_sent_at TIMESTAMP,
    confirmed_at TIMESTAMP,

    -- Unsubscribe
    unsubscribe_token VARCHAR(255) UNIQUE,
    unsubscribed_at TIMESTAMP,
    unsubscribe_reason TEXT,

    -- Bounce tracking
    bounce_count INT DEFAULT 0,
    last_bounce_at TIMESTAMP,
    bounce_type VARCHAR(50),  -- SOFT, HARD

    -- Complaint tracking
    complaint_count INT DEFAULT 0,
    last_complaint_at TIMESTAMP,

    -- Flexible custom fields (JSON)
    custom_fields JSONB,

    -- GDPR compliance
    gdpr_consent BOOLEAN DEFAULT FALSE,
    gdpr_consent_date TIMESTAMP,
    gdpr_ip_address VARCHAR(45),

    -- Metadata
    source VARCHAR(100),  -- WEB_FORM, CSV_IMPORT, API, MANUAL
    tags TEXT[],  -- Array for tag-based filtering

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_contact_per_user UNIQUE (user_id, email)
);

CREATE INDEX idx_contacts_user_id ON contacts(user_id);
CREATE INDEX idx_contacts_email ON contacts(email);
CREATE INDEX idx_contacts_status ON contacts(status);
CREATE INDEX idx_contacts_tags ON contacts USING GIN(tags);
CREATE INDEX idx_contacts_custom_fields ON contacts USING GIN(custom_fields);
```

**Status Enum:** PENDING, SUBSCRIBED, UNSUBSCRIBED, BOUNCED, COMPLAINED

**Custom Fields Example:**
```json
{
  "company": "Acme Corp",
  "job_title": "Software Engineer",
  "location": "New York",
  "birthday": "1990-05-15",
  "interests": ["tech", "ai", "startups"]
}
```

---

### 8. contact_list_memberships

**Purpose:** Many-to-many relationship between contacts and lists

```sql
CREATE TABLE contact_list_memberships (
    id BIGSERIAL PRIMARY KEY,
    contact_id BIGINT NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    list_id BIGINT NOT NULL REFERENCES contact_lists(id) ON DELETE CASCADE,

    status VARCHAR(50) DEFAULT 'ACTIVE',

    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_contact_list_membership UNIQUE (contact_id, list_id)
);

CREATE INDEX idx_memberships_contact ON contact_list_memberships(contact_id);
CREATE INDEX idx_memberships_list ON contact_list_memberships(list_id);
CREATE INDEX idx_memberships_status ON contact_list_memberships(status);
```

**Status Enum:** ACTIVE, REMOVED

---

### 9. segments

**Purpose:** Dynamic or static groups of contacts based on conditions

```sql
CREATE TABLE segments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    list_id BIGINT REFERENCES contact_lists(id) ON DELETE CASCADE,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Segment conditions (JSON query)
    conditions JSONB NOT NULL,

    is_dynamic BOOLEAN DEFAULT TRUE,  -- Dynamic = auto-update

    -- Cached count for performance
    cached_count INT DEFAULT 0,
    last_calculated_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_segments_user_id ON segments(user_id);
CREATE INDEX idx_segments_list_id ON segments(list_id);
```

**Conditions Example:**
```json
{
  "operator": "AND",
  "rules": [
    {"field": "tags", "operator": "contains", "value": "vip"},
    {"field": "custom_fields.location", "operator": "equals", "value": "New York"},
    {"field": "created_at", "operator": "greater_than", "value": "2024-01-01"}
  ]
}
```

---

### 10. campaign_recipients

**Purpose:** Track individual recipients for each campaign

```sql
CREATE TABLE campaign_recipients (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES email_campaigns(id) ON DELETE CASCADE,
    contact_id BIGINT NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,

    status VARCHAR(50) DEFAULT 'PENDING',

    -- Timestamps
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    bounced_at TIMESTAMP,
    complained_at TIMESTAMP,

    -- Engagement tracking
    open_count INT DEFAULT 0,
    click_count INT DEFAULT 0,

    -- Error handling
    error_message TEXT,
    retry_count INT DEFAULT 0,

    -- Tracking identifier
    tracking_id VARCHAR(255) UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_campaign_contact UNIQUE (campaign_id, contact_id)
);

CREATE INDEX idx_campaign_recipients_campaign ON campaign_recipients(campaign_id);
CREATE INDEX idx_campaign_recipients_contact ON campaign_recipients(contact_id);
CREATE INDEX idx_campaign_recipients_status ON campaign_recipients(status);
CREATE INDEX idx_campaign_recipients_tracking ON campaign_recipients(tracking_id);
```

**Status Enum:** PENDING, SENDING, SENT, DELIVERED, FAILED, BOUNCED, OPENED, CLICKED

---

### 11. campaign_links

**Purpose:** Track all links in a campaign for click analytics

```sql
CREATE TABLE campaign_links (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES email_campaigns(id) ON DELETE CASCADE,

    original_url TEXT NOT NULL,
    short_code VARCHAR(20) UNIQUE NOT NULL,  -- For /track/click/{shortCode}

    -- Analytics
    click_count INT DEFAULT 0,
    unique_click_count INT DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_campaign_links_campaign ON campaign_links(campaign_id);
CREATE INDEX idx_campaign_links_short_code ON campaign_links(short_code);
```

**Short Code Generation:** Random 8-character alphanumeric string

---

### 12. campaign_clicks

**Purpose:** Individual click events for detailed analytics

```sql
CREATE TABLE campaign_clicks (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES email_campaigns(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES campaign_recipients(id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES campaign_links(id) ON DELETE CASCADE,

    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Metadata
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX idx_campaign_clicks_campaign ON campaign_clicks(campaign_id);
CREATE INDEX idx_campaign_clicks_recipient ON campaign_clicks(recipient_id);
CREATE INDEX idx_campaign_clicks_link ON campaign_clicks(link_id);
CREATE INDEX idx_campaign_clicks_timestamp ON campaign_clicks(clicked_at);
```

---

### 13. api_keys

**Purpose:** API keys for programmatic access

```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    key_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(64) UNIQUE NOT NULL,  -- SHA-256 hash
    key_prefix VARCHAR(10) NOT NULL,  -- First 8 chars for identification

    -- Permissions
    scopes TEXT[],  -- ['read:contacts', 'write:contacts', 'send:campaigns']

    is_active BOOLEAN DEFAULT TRUE,

    -- Usage tracking
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_key ON api_keys(api_key);
CREATE INDEX idx_api_keys_active ON api_keys(is_active);
```

**Key Format:** `om_live_` or `om_test_` + 56 random characters

**Available Scopes:**
- `read:contacts`, `write:contacts`
- `read:campaigns`, `write:campaigns`, `send:campaigns`
- `read:templates`, `write:templates`
- `read:analytics`

---

### 14. rate_limits

**Purpose:** Track API rate limits per user/resource

```sql
CREATE TABLE rate_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    resource_type VARCHAR(50) NOT NULL,  -- API_REQUEST, EMAIL_SEND, SUBSCRIPTION

    -- Time window
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,

    -- Counts
    request_count INT DEFAULT 0,
    limit_value INT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_rate_limit_window UNIQUE (user_id, resource_type, window_start)
);

CREATE INDEX idx_rate_limits_user_resource ON rate_limits(user_id, resource_type, window_end);
```

**Note:** Primary rate limiting uses Redis. This table for audit/historical tracking only.

---

### 15. email_logs

**Purpose:** Comprehensive email sending logs

```sql
CREATE TABLE email_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    campaign_id BIGINT REFERENCES email_campaigns(id) ON DELETE SET NULL,
    recipient_id BIGINT REFERENCES campaign_recipients(id) ON DELETE SET NULL,
    provider_id BIGINT REFERENCES email_providers(id) ON DELETE SET NULL,

    email_type VARCHAR(50) NOT NULL,  -- CAMPAIGN, TRANSACTIONAL, CONFIRMATION

    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500),

    status VARCHAR(50) NOT NULL,  -- QUEUED, SENT, DELIVERED, BOUNCED, FAILED

    -- Provider response
    provider_message_id VARCHAR(255),
    error_message TEXT,

    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_logs_user_id ON email_logs(user_id);
CREATE INDEX idx_email_logs_campaign ON email_logs(campaign_id);
CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_created ON email_logs(created_at);
CREATE INDEX idx_email_logs_provider_message ON email_logs(provider_message_id);
```

**Retention:** Auto-delete logs older than 90 days (GDPR compliance)

---

### 16. webhook_events

**Purpose:** Store incoming webhook events from email providers

```sql
CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id BIGINT REFERENCES email_providers(id) ON DELETE SET NULL,

    event_type VARCHAR(100) NOT NULL,  -- DELIVERED, BOUNCED, OPENED, CLICKED, COMPLAINED
    provider_event_id VARCHAR(255),

    -- Raw payload
    payload JSONB NOT NULL,

    -- Processing status
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_events_user ON webhook_events(user_id);
CREATE INDEX idx_webhook_events_type ON webhook_events(event_type);
CREATE INDEX idx_webhook_events_processed ON webhook_events(processed);
CREATE INDEX idx_webhook_events_provider_event ON webhook_events(provider_event_id);
```

**Event Types:** DELIVERED, BOUNCED, OPENED, CLICKED, COMPLAINED, UNSUBSCRIBED

---

## Indexes Strategy

### Performance Optimizations

#### 1. Foreign Key Indexes
All foreign keys have indexes for efficient joins:
- `user_id` columns across all tables
- `campaign_id`, `contact_id`, `list_id` references

#### 2. Status Column Indexes
Frequently filtered by status:
- `users.account_status`
- `domains.status`
- `contacts.status`
- `email_campaigns.status`
- `campaign_recipients.status`

#### 3. Timestamp Indexes
For scheduled jobs and time-based queries:
- `email_campaigns.scheduled_at`
- `email_logs.created_at`
- `campaign_clicks.clicked_at`

#### 4. Unique Constraints
Prevent duplicates:
- `users.email`, `users.username`
- `domains.domain_name`
- `contacts (user_id, email)`
- `api_keys.api_key`
- `campaign_recipients (campaign_id, contact_id)`

#### 5. Specialized Indexes

**GIN Indexes (PostgreSQL):**
- `contacts.tags` - Array search
- `contacts.custom_fields` - JSONB queries
- `segments.conditions` - Complex filtering

**Composite Indexes:**
- `(user_id, resource_type, window_end)` for rate limiting
- `(is_active, is_default)` for email providers

---

## Data Retention Policies

### Automatic Cleanup (via CleanupScheduler)

#### Daily Cleanup (3 AM)
- **Unconfirmed Contacts:** Delete contacts with `status=PENDING` older than 30 days
- **Old Email Logs:** Delete logs older than 90 days
- **Processed Webhooks:** Delete webhook_events older than 30 days
- **Expired API Keys:** Mark expired keys as inactive

#### Monthly Cleanup (1st of month, 2 AM)
- **Reset Counters:** Reset `users.monthly_emails_sent` to 0
- **Archive Old Campaigns:** Move campaigns older than 2 years to archive table
- **Aggregate Analytics:** Summarize old click data, delete detailed records

### Manual Retention

#### GDPR Requests
- **Right to Deletion:** Anonymize or delete all user data within 30 days
- **Export Data:** Provide complete data dump in JSON format
- **Audit Trail:** Keep anonymized logs for legal compliance (7 years)

#### Configurable Policies
Users can configure retention in settings:
- Keep campaign reports: 1 year / 2 years / forever
- Detailed click logs: 30 days / 90 days / 1 year
- Contact history: 1 year / 3 years / forever

---

## Liquibase Migration Sequence

### Migration Files Order

```
src/main/resources/db/changelog/changes/
├── 001-create-users-table.xml              ✅ EXISTS
├── 002-create-email-templates-table.xml    ✅ EXISTS
├── 003-create-email-campaigns-table.xml    ✅ EXISTS
├── 004-update-users-table.xml              ⭐ NEW
├── 005-update-email-templates-table.xml    ⭐ NEW
├── 006-update-email-campaigns-table.xml    ⭐ NEW
├── 007-create-domains-table.xml            ⭐ NEW
├── 008-create-email-providers-table.xml    ⭐ NEW
├── 009-create-contact-lists-table.xml      ⭐ NEW
├── 010-create-contacts-table.xml           ⭐ NEW
├── 011-create-contact-list-memberships-table.xml  ⭐ NEW
├── 012-create-segments-table.xml           ⭐ NEW
├── 013-create-campaign-recipients-table.xml ⭐ NEW
├── 014-create-campaign-links-table.xml     ⭐ NEW
├── 015-create-campaign-clicks-table.xml    ⭐ NEW
├── 016-create-api-keys-table.xml           ⭐ NEW
├── 017-create-rate-limits-table.xml        ⭐ NEW
├── 018-create-email-logs-table.xml         ⭐ NEW
└── 019-create-webhook-events-table.xml     ⭐ NEW
```

### Master Changelog

Update `db.changelog-master.xml`:
```xml
<databaseChangeLog>
    <include file="db/changelog/changes/001-create-users-table.xml"/>
    <include file="db/changelog/changes/002-create-email-templates-table.xml"/>
    <include file="db/changelog/changes/003-create-email-campaigns-table.xml"/>
    <include file="db/changelog/changes/004-update-users-table.xml"/>
    <include file="db/changelog/changes/005-update-email-templates-table.xml"/>
    <include file="db/changelog/changes/006-update-email-campaigns-table.xml"/>
    <include file="db/changelog/changes/007-create-domains-table.xml"/>
    <!-- ... remaining migrations ... -->
</databaseChangeLog>
```

---

## Summary

**Database Stats:**
- **17 Tables Total** (3 existing + 14 new)
- **40+ Indexes** for optimized queries
- **JSONB Support** for flexible custom fields
- **Array Support** for tags and scopes
- **Foreign Keys** ensure referential integrity
- **Audit Timestamps** on all tables
- **GDPR Compliant** with consent tracking and retention policies

**Next Steps:**
1. Create Liquibase migration files (004-019)
2. Run migrations: `mvn liquibase:update`
3. Create JPA entities matching schema
4. Implement repositories with custom queries

---

**Next:** See [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) for API specifications, security implementation, and development roadmap.
