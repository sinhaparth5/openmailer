# OpenMailer - System Architecture

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Overview](#system-overview)
3. [Technology Stack](#technology-stack)
4. [Architecture Design](#architecture-design)
5. [Package Structure](#package-structure)
6. [Key Architectural Decisions](#key-architectural-decisions)

---

## Executive Summary

**OpenMailer** is an open-source, self-hosted email marketing platform designed as a robust alternative to Mailchimp. Built with Spring Boot and PostgreSQL, it provides enterprise-grade features including:

- **Custom Domain Email Sending** with DNS verification (SPF, DKIM, DMARC)
- **Multi-Provider Support** (AWS SES, SendGrid, Custom SMTP)
- **Advanced Contact Management** with segmentation, tagging, and CSV import/export
- **WYSIWYG Email Templates** using Quill.js editor
- **Campaign Scheduling** with cron job automation
- **Comprehensive Analytics** with open and click tracking
- **REST API** for website subscription forms and integrations
- **Enterprise Security** with JWT authentication, API keys, and rate limiting
- **GDPR Compliance** with consent management and data portability

**Current Status:** Foundation phase - database schema and basic entities exist, ready for service layer implementation.

---

## System Overview

### Core Features

#### 1. Domain Management & Verification
- Users can register custom domains for sending emails
- Automatic DNS record generation (SPF, DKIM, DMARC)
- Periodic verification via cron jobs
- DKIM key pair generation and secure storage
- Domain reputation tracking

#### 2. Email Provider Abstraction
- Support for multiple email sending providers:
  - **AWS SES** (Amazon Simple Email Service)
  - **SendGrid** (popular email API)
  - **Custom SMTP** (any SMTP server)
- Provider failover and load balancing
- Encrypted credential storage
- Per-provider rate limiting and monitoring

#### 3. Contact List Management
- Unlimited contact lists per user
- Custom fields (JSONB storage for flexibility)
- Tag-based organization
- CSV bulk import/export
- Advanced search and filtering
- Duplicate detection
- GDPR-compliant data handling

#### 4. Segmentation
- Dynamic segments with real-time updates
- Condition-based filtering (tags, custom fields, engagement)
- Static segments for manual curation
- Segment analytics and insights

#### 5. Email Templates
- WYSIWYG editor (Quill.js)
- Raw HTML support for advanced users
- Template variables for personalization
- Preview with sample data
- Version control
- Template library (system + custom)

#### 6. Campaign Management
- Create, schedule, and send email campaigns
- Recipient targeting (lists + segments)
- A/B testing support
- Send speed control (throttling)
- Automatic retry for failed sends
- Real-time sending progress

#### 7. Analytics & Tracking
- Email open tracking (transparent pixel)
- Link click tracking (URL shortening)
- Bounce tracking (hard vs soft)
- Spam complaint tracking
- Engagement metrics (open rate, click rate)
- Campaign performance reports
- Exportable analytics data

#### 8. Subscription Management
- Double opt-in confirmation flow
- One-click unsubscribe
- Preference center for subscribers
- Webhook events for integrations
- Public API for website forms

#### 9. Security & Compliance
- JWT-based authentication for web users
- API key authentication for programmatic access
- Role-based access control (RBAC)
- Rate limiting (per user, per IP, per API key)
- Two-factor authentication (TOTP)
- Encryption at rest for sensitive data
- GDPR compliance features (consent tracking, data export, right to deletion)

---

## Technology Stack

### Backend Framework
- **Spring Boot 4.0** - Main application framework
- **Java 25** - Latest LTS version
- **Maven** - Dependency management and build tool

### Database
- **PostgreSQL** - Primary relational database
  - JSONB support for flexible custom fields
  - Advanced indexing (B-tree, GIN)
  - Full-text search capabilities
- **Liquibase** - Database migration and version control

### Security
- **Spring Security** - Authentication and authorization
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing (strength 12)
- **AES-256** - Encryption for sensitive data (credentials, DKIM keys)

### Email Services Integration
- **AWS SDK for SES** - Amazon email service
- **SendGrid Java SDK** - SendGrid integration
- **Spring Mail** - SMTP support
- **dnsjava** - DNS lookup and verification
- **utils-mail-dkim** - DKIM signing implementation

### Caching & Performance
- **Redis** - In-memory data store for:
  - Rate limiting counters
  - Session storage
  - Segment count caching
  - Queue management

### Frontend
- **Thymeleaf** - Server-side template engine
- **Tailwind CSS 3.4** - Utility-first CSS framework
- **Quill.js** - WYSIWYG rich text editor
- **Chart.js** - Analytics visualizations (planned)

### Data Processing
- **OpenCSV** - CSV import/export
- **Jackson** - JSON serialization/deserialization
- **MapStruct** - DTO mapping

### Monitoring & Operations
- **Spring Boot Actuator** - Health checks and metrics
- **SLF4J + Logback** - Structured logging
- **Micrometer** - Application metrics

### Development Tools
- **Lombok** - Reduce boilerplate code
- **Maven Checkstyle** - Code quality enforcement (Google style)
- **JUnit 5** - Unit testing
- **Testcontainers** - Integration testing with Docker

---

## Architecture Design

### Layered Architecture

OpenMailer follows a clean, layered architecture pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│                                                                 │
│  ┌──────────────────────┐    ┌──────────────────────┐        │
│  │   Web Controllers    │    │   REST API           │        │
│  │   (Thymeleaf)       │    │   Controllers        │        │
│  └──────────────────────┘    └──────────────────────┘        │
│                                                                 │
│  ┌──────────────────────────────────────────────────┐        │
│  │   DTOs (Request/Response Objects)                │        │
│  │   Exception Handlers                              │        │
│  └──────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                              │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Domain     │  │   Campaign   │  │   Contact    │       │
│  │   Service    │  │   Service    │  │   Service    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                                 │
│  • Business Logic                                              │
│  • Transaction Management                                      │
│  • Orchestration between repositories                          │
│  • Validation                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                             │
│                                                                 │
│  ┌──────────────────────────────────────────────────┐        │
│  │   Spring Data JPA Repositories                   │        │
│  │   Custom Query Methods                            │        │
│  │   Specifications for Complex Queries              │        │
│  └──────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE LAYER                           │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Email      │  │   DNS        │  │   Encryption │       │
│  │   Providers  │  │   Service    │  │   Service    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐                          │
│  │   Cache      │  │   Schedulers │                          │
│  │   (Redis)    │  │   (Cron)     │                          │
│  └──────────────┘  └──────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                        ┌──────────┐
                        │ Database │
                        │(PostgreSQL)│
                        └──────────┘
```

### Design Patterns

#### 1. Repository Pattern
All database access goes through Spring Data JPA repositories, providing abstraction and testability.

```java
public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByUserIdAndEmail(Long userId, String email);
    List<Contact> findByUserIdAndStatus(Long userId, ContactStatus status, Pageable pageable);
}
```

#### 2. Service Layer Pattern
Business logic encapsulated in service classes, promoting reusability and separation of concerns.

```java
@Service
@Transactional
public class ContactService {
    private final ContactRepository contactRepository;
    private final ContactListService listService;
    // Business logic here
}
```

#### 3. Factory Pattern
Email provider abstraction using factory to instantiate correct provider.

```java
@Component
public class ProviderFactory {
    public EmailSender createProvider(EmailProviderType type, String config) {
        return switch(type) {
            case AWS_SES -> new AwsSesProvider(config);
            case SENDGRID -> new SendGridProvider(config);
            case SMTP -> new SmtpProvider(config);
        };
    }
}
```

#### 4. Strategy Pattern
Different email sending strategies based on provider type.

```java
public interface EmailSender {
    SendResult send(EmailMessage message);
    boolean testConnection();
    ProviderStats getStats();
}
```

#### 5. DTO Pattern
Separation between domain models and API contracts using Data Transfer Objects.

```java
public record ContactCreateRequest(
    String email,
    String firstName,
    String lastName,
    Map<String, Object> customFields
) {}
```

#### 6. Observer Pattern
Webhook events processed asynchronously, notifying relevant services.

```java
@Async
public void processWebhookEvent(WebhookEvent event) {
    // Handle bounce, complaint, delivery events
}
```

---

## Package Structure

```
com.openmailer.openmailer/
│
├── config/                          # Configuration classes
│   ├── SecurityConfig.java          # Spring Security setup
│   ├── AsyncConfig.java             # @Async thread pool config
│   ├── CacheConfig.java             # Redis cache configuration
│   ├── SchedulingConfig.java        # @Scheduled config
│   ├── EncryptionConfig.java        # AES encryption beans
│   └── RateLimitConfig.java         # Rate limiting setup
│
├── model/                           # JPA Entity models
│   ├── User.java
│   ├── EmailTemplate.java
│   ├── EmailCampaign.java
│   ├── Domain.java
│   ├── EmailProvider.java
│   ├── ContactList.java
│   ├── Contact.java
│   ├── ContactListMembership.java
│   ├── Segment.java
│   ├── CampaignRecipient.java
│   ├── CampaignLink.java
│   ├── CampaignClick.java
│   ├── ApiKey.java
│   ├── RateLimit.java
│   ├── EmailLog.java
│   └── WebhookEvent.java
│
├── dto/                             # Data Transfer Objects
│   ├── request/                     # API request DTOs
│   │   ├── auth/
│   │   ├── domain/
│   │   ├── provider/
│   │   ├── contact/
│   │   ├── list/
│   │   ├── segment/
│   │   ├── template/
│   │   ├── campaign/
│   │   └── api/
│   └── response/                    # API response DTOs
│       ├── auth/
│       ├── domain/
│       ├── contact/
│       ├── campaign/
│       └── common/
│
├── repository/                      # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── EmailTemplateRepository.java
│   ├── EmailCampaignRepository.java
│   ├── DomainRepository.java
│   ├── EmailProviderRepository.java
│   ├── ContactRepository.java
│   ├── ContactListRepository.java
│   ├── SegmentRepository.java
│   ├── CampaignRecipientRepository.java
│   ├── ApiKeyRepository.java
│   └── [... 14 total repositories]
│
├── service/                         # Business logic services
│   ├── auth/
│   │   ├── AuthenticationService.java
│   │   ├── UserService.java
│   │   ├── JwtService.java
│   │   └── TwoFactorService.java
│   ├── domain/
│   │   ├── DomainService.java
│   │   └── DnsVerificationService.java
│   ├── provider/
│   │   ├── EmailProviderService.java
│   │   ├── ProviderFactory.java
│   │   └── providers/
│   │       ├── EmailSender.java (interface)
│   │       ├── AwsSesProvider.java
│   │       ├── SendGridProvider.java
│   │       └── SmtpProvider.java
│   ├── contact/
│   │   ├── ContactService.java
│   │   ├── ContactListService.java
│   │   ├── ContactImportService.java
│   │   └── SubscriptionService.java
│   ├── segment/
│   │   └── SegmentService.java
│   ├── template/
│   │   ├── EmailTemplateService.java
│   │   └── TemplateRenderer.java
│   ├── campaign/
│   │   ├── CampaignService.java
│   │   ├── CampaignSendingService.java
│   │   ├── CampaignSchedulerService.java
│   │   └── TrackingService.java
│   ├── security/
│   │   ├── EncryptionService.java
│   │   ├── RateLimitService.java
│   │   └── SpamPreventionService.java
│   └── webhook/
│       └── WebhookService.java
│
├── controller/                      # HTTP controllers
│   ├── web/                         # Thymeleaf MVC controllers
│   │   ├── DashboardController.java
│   │   ├── ContactWebController.java
│   │   ├── TemplateWebController.java
│   │   └── CampaignWebController.java
│   ├── api/                         # REST API controllers
│   │   ├── AuthController.java
│   │   ├── DomainController.java
│   │   ├── ContactController.java
│   │   ├── CampaignController.java
│   │   └── [... 10+ API controllers]
│   └── webhook/
│       ├── TrackingController.java  # Open/click tracking
│       └── WebhookController.java   # Provider webhooks
│
├── security/                        # Security components
│   ├── JwtAuthenticationFilter.java
│   ├── ApiKeyAuthenticationFilter.java
│   ├── RateLimitFilter.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
│
├── scheduler/                       # Cron job schedulers
│   ├── CampaignScheduler.java
│   ├── DnsVerificationScheduler.java
│   ├── BounceProcessingScheduler.java
│   ├── AnalyticsAggregationScheduler.java
│   ├── CleanupScheduler.java
│   └── SegmentRefreshScheduler.java
│
├── exception/                       # Custom exceptions
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── RateLimitExceededException.java
│   └── [... custom exceptions]
│
├── util/                           # Utility classes
│   ├── EmailValidator.java
│   ├── CsvUtils.java
│   ├── EncryptionUtils.java
│   ├── DkimUtils.java
│   └── TrackingPixelGenerator.java
│
└── constants/                      # Enums and constants
    ├── ContactStatus.java
    ├── CampaignStatus.java
    ├── EmailProviderType.java
    └── ApiScopes.java
```

---

## Key Architectural Decisions

### 1. Multi-Provider Email Sending

**Decision:** Support multiple email sending providers with abstraction layer

**Rationale:**
- **Vendor Independence:** Avoid lock-in to a single provider
- **Reliability:** Automatic failover if primary provider fails
- **Cost Optimization:** Use cheaper providers for bulk, premium for transactional
- **Deliverability:** Diversify sending sources to improve reputation
- **User Choice:** Let users bring their own provider credentials

**Implementation:**
```java
public interface EmailSender {
    SendResult send(EmailMessage message);
    boolean testConnection();
}

// Concrete implementations for each provider
public class AwsSesProvider implements EmailSender { ... }
public class SendGridProvider implements EmailSender { ... }
public class SmtpProvider implements EmailSender { ... }
```

**Trade-offs:**
- ✅ Flexibility and resilience
- ✅ Easy to add new providers
- ❌ Increased complexity
- ❌ Must handle provider-specific quirks

---

### 2. DNS Verification Approach

**Decision:** Manual DNS setup with automated verification

**Rationale:**
- **Universal Compatibility:** Works with any DNS provider
- **User Control:** Users maintain full control of their domains
- **Simplicity:** No need to integrate with dozens of DNS provider APIs
- **Industry Standard:** Same approach used by Mailchimp, SendGrid, etc.
- **Security:** No need to store DNS provider credentials

**Process:**
1. User adds domain to OpenMailer
2. System generates SPF, DKIM, DMARC records
3. User manually adds records to their DNS
4. System verifies records via DNS queries (cron job)
5. Domain activated upon successful verification

**Alternative Considered:** Automatic DNS management via APIs (CloudFlare, Route53)
- **Rejected because:** Limited to specific providers, complex, security concerns

---

### 3. Contact Data Model

**Decision:** Relational database (PostgreSQL) with JSONB for custom fields

**Rationale:**
- **ACID Compliance:** Email marketing requires data integrity
- **Complex Queries:** Segments need sophisticated filtering
- **Relationships:** Many-to-many (contacts ↔ lists, campaigns ↔ recipients)
- **Flexibility:** JSONB allows unlimited custom fields without schema changes
- **Performance:** PostgreSQL handles millions of contacts efficiently

**Schema:**
```sql
contacts (
    id, user_id, email, first_name, last_name, status,
    custom_fields JSONB,  -- Flexible custom data
    tags TEXT[],          -- Array for tag-based filtering
    ...
)
```

**Alternative Considered:** NoSQL (MongoDB) for contacts
- **Rejected because:** Segments require complex joins, ACID properties critical

---

### 4. Campaign Sending Strategy

**Decision:** Asynchronous batch processing with queue

**Rationale:**
- **Non-blocking:** API responds immediately, campaign processes in background
- **Scalability:** Process campaigns in parallel across multiple workers
- **Rate Limiting:** Precise control over sending speed (emails/minute)
- **Fault Tolerance:** Failed batches can be retried
- **Progress Tracking:** Real-time updates on sending progress

**Implementation:**
```java
@Async
public void processCampaign(Long campaignId) {
    // 1. Load recipients in batches (1000 per batch)
    // 2. Process batches in parallel
    // 3. Respect send_speed limit (throttling)
    // 4. Update campaign statistics in real-time
    // 5. Handle failures with retry logic
}
```

**Queue Options:**
- **Phase 1:** Database-backed queue (campaign_recipients table)
- **Phase 2+:** Redis queue or RabbitMQ for high-volume

---

### 5. Tracking Implementation

**Decision:** Self-hosted tracking (not third-party services)

**Rationale:**
- **Privacy:** All tracking data stays on user's server
- **Cost:** No per-event charges from third-party services
- **Performance:** No external API latency
- **Customization:** Full control over tracking logic
- **Reliability:** Not dependent on external services

**Mechanisms:**
- **Open Tracking:** 1x1 transparent pixel embedded in HTML
  - URL: `https://openmailer.com/track/open/{trackingId}`
  - Returns 1px transparent GIF
  - Records timestamp, IP, user agent

- **Click Tracking:** URL redirection through short links
  - Original: `https://example.com/product`
  - Tracked: `https://openmailer.com/track/click/{shortCode}`
  - Records click, then redirects to original URL

---

### 6. Authentication Strategy

**Decision:** JWT for web users + API keys for programmatic access

**Rationale:**
- **JWT for Web:**
  - Stateless (no server-side sessions)
  - Scalable (works across multiple servers)
  - Short-lived (15 min) with refresh tokens (7 days)
  - Standard format (RFC 7519)

- **API Keys for Automation:**
  - Long-lived credentials for applications
  - Revocable at any time
  - Scoped permissions (read:contacts, send:campaigns)
  - Rate limiting per key

**Format:**
- JWT: Standard bearer token in Authorization header
- API Keys: `om_live_64characterhash` or `om_test_64characterhash`

**Security:**
- JWT signed with HS512 algorithm
- API keys stored as SHA-256 hash (one-way)
- All tokens transmitted over HTTPS only

---

### 7. Rate Limiting Strategy

**Decision:** Redis-based sliding window algorithm

**Rationale:**
- **Performance:** In-memory counters (sub-millisecond lookups)
- **Accuracy:** Sliding window prevents burst traffic
- **Distributed:** Works across multiple application instances
- **Automatic Cleanup:** Redis TTL expires old counters
- **Flexible:** Different limits per resource type

**Limits:**
- **API Requests:** 100/minute per user
- **Email Sends:** Based on subscription plan
- **Login Attempts:** 10/hour per IP address
- **Subscription Forms:** 1000/day per domain

**Implementation:**
```java
// Redis key: rate_limit:{userId}:{resource}:{windowStart}
// Value: request count
// TTL: window duration
```

---

### 8. Data Encryption

**Decision:** Encrypt sensitive data at rest using AES-256

**Rationale:**
- **Compliance:** Required for GDPR, SOC 2
- **Security:** Protects against database breaches
- **User Trust:** Email credentials and DKIM keys must be secure

**What Gets Encrypted:**
- Email provider API keys/credentials
- DKIM private keys
- Two-factor authentication secrets
- API key raw values (also hashed)

**Key Management:**
- Encryption key stored in environment variable
- Key rotation supported (re-encrypt with new key)
- Uses Spring Security Crypto module

**What's NOT Encrypted:**
- Contact emails (needed for queries/indexing)
- Campaign content (needed for search)
- Analytics data (aggregated, not sensitive)

---

### 9. GDPR Compliance

**Decision:** Built-in GDPR features from day one

**Rationale:**
- **Legal Requirement:** Mandatory for EU users
- **User Rights:** Respect for privacy builds trust
- **Competitive Advantage:** Many tools lack proper compliance
- **Future-Proof:** Privacy regulations expanding globally

**Features:**
- **Consent Management:**
  - Double opt-in by default
  - Track consent timestamp, IP, method
  - Separate consent storage from contact data

- **Data Subject Rights:**
  - Right to access (export all data as JSON)
  - Right to deletion (anonymize or delete)
  - Right to portability (machine-readable format)
  - Right to rectification (update personal data)

- **Data Retention:**
  - Configurable retention periods
  - Automatic cleanup of old data
  - Audit trail for all changes

---

### 10. Scalability Design

**Decision:** Stateless application with horizontal scaling support

**Architecture Characteristics:**
- **Stateless:** JWT authentication (no server-side sessions)
- **Shared State:** Redis for cache and rate limiting
- **Database:** PostgreSQL with connection pooling
- **Async Processing:** Campaign sending doesn't block web requests
- **Caching:** Frequently accessed data cached in Redis

**Scaling Path:**
- **Phase 1 (0-10K users):** Single server with PostgreSQL + Redis
- **Phase 2 (10K-100K):** Load balancer + multiple app servers
- **Phase 3 (100K+):** Database read replicas, message queue (RabbitMQ)

**Performance Targets:**
- Support 1M+ contacts per user
- Send 100K+ emails/hour per user (provider dependent)
- API response time < 200ms (p95)
- Campaign scheduling accuracy ±1 minute

---

## Summary

OpenMailer's architecture prioritizes:
- ✅ **Modularity:** Clean separation of concerns
- ✅ **Flexibility:** Multi-provider support, custom fields
- ✅ **Security:** Encryption, authentication, GDPR compliance
- ✅ **Scalability:** Stateless design, async processing
- ✅ **Reliability:** Failover, retry logic, error handling
- ✅ **Developer Experience:** Clear structure, standard patterns

The system is designed to start simple (single server deployment) while supporting future growth to enterprise scale.

---

**Next:** See [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) for complete database design and [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) for API specs and implementation roadmap.
