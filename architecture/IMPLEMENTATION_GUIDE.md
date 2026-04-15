# OpenMailer - Implementation Guide

## Table of Contents
1. [API Specifications](#api-specifications)
2. [Security Implementation](#security-implementation)
3. [Email Deliverability](#email-deliverability)
4. [Implementation Roadmap](#implementation-roadmap)
5. [Dependencies](#dependencies)
6. [Scalability & Performance](#scalability--performance)
7. [Monitoring & Operations](#monitoring--operations)
8. [Testing Strategy](#testing-strategy)

---

## API Specifications

### Base URL Structure

```
Production:  https://api.openmailer.com/api/v1
Development: http://localhost:8080/api/v1
```

### Authentication Methods

#### 1. JWT Bearer Token (Web Users)
```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### 2. API Key (Programmatic Access)
```http
X-API-Key: om_live_abc123...
```

### Common Response Formats

**Success Response:**
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Contact not found",
    "field": "email"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Paginated Response:**
```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 1,
    "size": 50,
    "total": 1250,
    "totalPages": 25
  }
}
```

---

### Authentication Endpoints

#### POST /auth/register
Register new user account

**Request:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:** 201 Created
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "USER",
    "subscriptionPlan": "FREE"
  }
}
```

---

#### POST /auth/login
Authenticate and receive JWT tokens

**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "twoFactorCode": "123456"  // Optional, if 2FA enabled
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "refresh_abc123...",
    "expiresIn": 900,  // 15 minutes
    "user": {
      "id": 1,
      "email": "john@example.com",
      "username": "john_doe"
    }
  }
}
```

---

#### POST /auth/refresh
Refresh expired access token

**Request:**
```json
{
  "refreshToken": "refresh_abc123..."
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "expiresIn": 900
  }
}
```

---

### Domain Management Endpoints

#### POST /domains
Add custom domain for email sending

**Request:**
```json
{
  "domainName": "mail.example.com"
}
```

**Response:** 201 Created
```json
{
  "success": true,
  "data": {
    "id": 1,
    "domainName": "mail.example.com",
    "status": "PENDING",
    "dnsRecords": {
      "spf": {
        "type": "TXT",
        "name": "@",
        "value": "v=spf1 include:openmailer.com ~all"
      },
      "dkim": {
        "type": "TXT",
        "name": "openmailer._domainkey",
        "value": "k=rsa; p=MIGfMA0GCSqGSIb3..."
      },
      "dmarc": {
        "type": "TXT",
        "name": "_dmarc",
        "value": "v=DMARC1; p=quarantine; rua=mailto:dmarc@openmailer.com"
      }
    }
  }
}
```

---

#### GET /domains/{id}/dns-records
Get DNS records to add to domain

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "instructions": "Add these DNS records to your domain provider",
    "records": [
      {
        "type": "TXT",
        "name": "@",
        "value": "v=spf1 include:openmailer.com ~all",
        "verified": false
      },
      {
        "type": "TXT",
        "name": "openmailer._domainkey",
        "value": "k=rsa; p=MIGfMA0...",
        "verified": false
      },
      {
        "type": "TXT",
        "name": "_dmarc",
        "value": "v=DMARC1; p=quarantine...",
        "verified": false
      }
    ]
  }
}
```

---

#### POST /domains/{id}/verify
Trigger manual domain verification

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "domainName": "mail.example.com",
    "status": "VERIFIED",
    "spfVerified": true,
    "dkimVerified": true,
    "dmarcVerified": true,
    "verifiedAt": "2025-01-15T10:30:00Z"
  }
}
```

---

### Contact Management Endpoints

#### POST /contacts
Create single contact

**Request:**
```json
{
  "email": "jane@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "customFields": {
    "company": "Acme Corp",
    "jobTitle": "Marketing Manager"
  },
  "tags": ["vip", "newsletter"],
  "gdprConsent": true,
  "gdprIpAddress": "192.168.1.1"
}
```

**Response:** 201 Created
```json
{
  "success": true,
  "data": {
    "id": 100,
    "email": "jane@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "status": "PENDING",
    "customFields": { ... },
    "tags": ["vip", "newsletter"],
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

---

#### GET /contacts
List contacts with pagination and filtering

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 50, max: 100)
- `status` (SUBSCRIBED, UNSUBSCRIBED, BOUNCED, etc.)
- `tags` (comma-separated)
- `search` (search email, first_name, last_name)

**Example:** `/contacts?page=0&size=50&status=SUBSCRIBED&tags=vip&search=jane`

**Response:** 200 OK
```json
{
  "success": true,
  "data": [
    {
      "id": 100,
      "email": "jane@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "status": "SUBSCRIBED",
      "tags": ["vip", "newsletter"]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "total": 1250,
    "totalPages": 25
  }
}
```

---

#### POST /contacts/import
Bulk import contacts from CSV

**Request:** multipart/form-data
```
file: contacts.csv
listId: 5
skipDuplicates: true
sendConfirmation: true
```

**CSV Format:**
```csv
email,first_name,last_name,custom.company,custom.job_title,tags
jane@example.com,Jane,Smith,Acme Corp,Manager,"vip,newsletter"
john@example.com,John,Doe,Example Inc,Developer,"developer"
```

**Response:** 202 Accepted
```json
{
  "success": true,
  "data": {
    "jobId": "import_abc123",
    "status": "PROCESSING",
    "message": "Import started. You'll receive an email when complete."
  }
}
```

---

#### GET /contacts/export
Export contacts to CSV

**Query Parameters:**
- `listId` (optional)
- `segmentId` (optional)
- `status` (optional)
- `format` (csv, json)

**Response:** 200 OK
```csv
email,first_name,last_name,status,tags,created_at
jane@example.com,Jane,Smith,SUBSCRIBED,"vip,newsletter",2025-01-15T10:30:00Z
```

---

### Contact List Endpoints

#### POST /lists
Create contact list

**Request:**
```json
{
  "name": "Newsletter Subscribers",
  "description": "Monthly newsletter recipients",
  "doubleOptInEnabled": true
}
```

**Response:** 201 Created
```json
{
  "success": true,
  "data": {
    "id": 5,
    "name": "Newsletter Subscribers",
    "totalContacts": 0,
    "activeContacts": 0,
    "doubleOptInEnabled": true
  }
}
```

---

#### POST /lists/{id}/contacts
Add contacts to list

**Request:**
```json
{
  "contactIds": [100, 101, 102]
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "added": 3,
    "skipped": 0,
    "errors": []
  }
}
```

---

### Campaign Endpoints

#### POST /campaigns
Create new campaign

**Request:**
```json
{
  "name": "January Newsletter",
  "templateId": 10,
  "listId": 5,
  "segmentId": null,
  "subjectLine": "Welcome to 2025!",
  "previewText": "Check out our latest updates",
  "fromName": "John from OpenMailer",
  "fromEmail": "john@mail.example.com",
  "replyToEmail": "reply@example.com",
  "domainId": 1,
  "providerId": 2,
  "trackOpens": true,
  "trackClicks": true,
  "sendSpeed": 100
}
```

**Response:** 201 Created
```json
{
  "success": true,
  "data": {
    "id": 50,
    "name": "January Newsletter",
    "status": "DRAFT",
    "totalRecipients": 1250,
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

---

#### POST /campaigns/{id}/schedule
Schedule campaign for future sending

**Request:**
```json
{
  "scheduledAt": "2025-01-20T09:00:00Z"
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "id": 50,
    "status": "SCHEDULED",
    "scheduledAt": "2025-01-20T09:00:00Z"
  }
}
```

---

#### POST /campaigns/{id}/send
Send campaign immediately

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "id": 50,
    "status": "SENDING",
    "totalRecipients": 1250,
    "message": "Campaign is being sent. Check analytics for progress."
  }
}
```

---

#### GET /campaigns/{id}/stats
Get campaign statistics

**Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "campaignId": 50,
    "campaignName": "January Newsletter",
    "status": "SENT",
    "totalRecipients": 1250,
    "sentCount": 1245,
    "deliveredCount": 1230,
    "failedCount": 5,
    "openedCount": 650,
    "clickedCount": 180,
    "bouncedCount": 10,
    "complainedCount": 2,
    "unsubscribedCount": 5,
    "openRate": 52.85,
    "clickRate": 14.63,
    "bounceRate": 0.81,
    "sentAt": "2025-01-15T10:00:00Z"
  }
}
```

---

### Public Subscription API

#### POST /subscribe
Subscribe to mailing list (public endpoint)

**Request:**
```json
{
  "email": "subscriber@example.com",
  "firstName": "Sarah",
  "lastName": "Johnson",
  "listId": 5,
  "customFields": {
    "source": "homepage_footer"
  },
  "gdprConsent": true
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "message": "Please check your email to confirm subscription."
}
```

---

#### POST /confirm/{token}
Confirm email subscription (double opt-in)

**Response:** 200 OK
```json
{
  "success": true,
  "message": "Your subscription has been confirmed. Welcome!"
}
```

---

#### POST /unsubscribe/{token}
Unsubscribe from all lists

**Request:**
```json
{
  "reason": "Too many emails"  // Optional
}
```

**Response:** 200 OK
```json
{
  "success": true,
  "message": "You have been unsubscribed successfully."
}
```

---

### Tracking Endpoints

#### GET /track/open/{trackingId}
Track email open (returns 1x1 transparent GIF)

**Response:** 200 OK
- Content-Type: image/gif
- Cache-Control: no-cache, no-store, must-revalidate
- Returns 1px transparent GIF

---

#### GET /track/click/{shortCode}
Track link click and redirect

**Response:** 302 Found
- Location: {original_url}
- Records click event and redirects to original URL

---

## Security Implementation

### Password Security

**BCrypt Hashing:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // Strength: 12
}
```

**Password Requirements:**
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character

---

### JWT Configuration

**Token Structure:**
```java
// Access Token (15 min expiry)
{
  "sub": "john@example.com",
  "userId": 1,
  "role": "USER",
  "iat": 1642234567,
  "exp": 1642235467
}

// Refresh Token (7 days expiry)
{
  "sub": "john@example.com",
  "type": "REFRESH",
  "iat": 1642234567,
  "exp": 1642839367
}
```

**Implementation:**
```java
@Service
public class JwtService {
    private static final String SECRET = System.getenv("JWT_SECRET");
    private static final long ACCESS_TOKEN_EXPIRY = 15 * 60 * 1000;  // 15 min
    private static final long REFRESH_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000;  // 7 days

    public String generateAccessToken(User user) {
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(SignatureAlgorithm.HS512, SECRET)
            .compact();
    }
}
```

---

### API Key Authentication

**Key Generation:**
```java
public String generateApiKey() {
    String prefix = "om_live_";
    String randomPart = generateSecureRandom(56);
    return prefix + randomPart;
}

public String hashApiKey(String apiKey) {
    return DigestUtils.sha256Hex(apiKey);  // Store hash, not plain text
}
```

**Authentication Filter:**
```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            String hashedKey = hashApiKey(apiKey);
            Optional<ApiKey> key = apiKeyRepository.findByApiKey(hashedKey);
            if (key.isPresent() && key.get().isActive()) {
                // Authenticate user
                checkScopes(key.get(), request);
            }
        }
        chain.doFilter(request, response);
    }
}
```

---

### Rate Limiting

**Redis-Based Implementation:**
```java
@Service
public class RateLimitService {
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    public boolean allowRequest(Long userId, String resource, int limit, Duration window) {
        String key = String.format("rate_limit:%d:%s:%d",
                                   userId, resource, System.currentTimeMillis() / window.toMillis());

        Integer count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            count = 0;
        }

        if (count >= limit) {
            return false;  // Rate limit exceeded
        }

        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, window);
        return true;
    }
}
```

**Rate Limits:**
```java
public class RateLimits {
    public static final int API_REQUESTS_PER_MINUTE = 100;
    public static final int LOGIN_ATTEMPTS_PER_HOUR = 10;
    public static final int EMAIL_SENDS_PER_HOUR = 1000;  // Adjustable by plan
    public static final int SUBSCRIPTIONS_PER_DAY = 1000;
}
```

---

### Data Encryption

**Sensitive Data Encryption:**
```java
@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private final SecretKey secretKey;

    public EncryptionService() {
        String keyString = System.getenv("ENCRYPTION_KEY");
        this.secretKey = new SecretKeySpec(keyString.getBytes(), "AES");
    }

    public String encrypt(String plaintext) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String ciphertext) {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decrypted);
    }
}
```

**What Gets Encrypted:**
- Email provider API keys
- SMTP passwords
- DKIM private keys
- Two-factor secrets

---

### CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("https://*.openmailer.com");
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

---

### GDPR Compliance Features

#### Data Export
```java
@GetMapping("/auth/export-data")
public ResponseEntity<byte[]> exportUserData() {
    User user = getCurrentUser();

    Map<String, Object> userData = new HashMap<>();
    userData.put("user", user);
    userData.put("contacts", contactService.findAllByUser(user.getId()));
    userData.put("campaigns", campaignService.findAllByUser(user.getId()));
    userData.put("templates", templateService.findAllByUser(user.getId()));

    String json = objectMapper.writeValueAsString(userData);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=user_data.json")
        .body(json.getBytes());
}
```

#### Data Deletion
```java
@DeleteMapping("/auth/delete-account")
public ResponseEntity<?> deleteAccount(@RequestParam String confirmation) {
    if (!"DELETE".equals(confirmation)) {
        throw new ValidationException("Confirmation required");
    }

    User user = getCurrentUser();

    // Anonymize or delete data
    contactService.deleteAllByUser(user.getId());
    campaignService.deleteAllByUser(user.getId());
    // ... delete all related data

    userService.delete(user.getId());

    return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
}
```

---

## Email Deliverability

### DNS Record Setup

#### SPF (Sender Policy Framework)
```
Type: TXT
Name: @
Value: v=spf1 include:_spf.openmailer.com ~all
```

**Purpose:** Specifies which mail servers can send email from your domain

---

#### DKIM (DomainKeys Identified Mail)
```
Type: TXT
Name: openmailer._domainkey
Value: v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC...
```

**Purpose:** Cryptographically signs emails to prevent tampering

**Key Generation:**
```java
@Service
public class DkimUtils {
    public KeyPair generateDkimKeys() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public String getPublicKeyString(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }
}
```

---

#### DMARC (Domain-based Message Authentication)
```
Type: TXT
Name: _dmarc
Value: v=DMARC1; p=quarantine; rua=mailto:dmarc@openmailer.com; pct=100; adkim=s; aspf=s
```

**Purpose:** Tells receiving servers what to do with emails that fail SPF/DKIM

---

### DNS Verification Process

```java
@Service
public class DnsVerificationService {
    public boolean verifySpf(String domain) {
        List<String> txtRecords = queryDnsTxtRecords(domain);
        return txtRecords.stream()
            .anyMatch(record -> record.contains("v=spf1") &&
                               record.contains("include:_spf.openmailer.com"));
    }

    public boolean verifyDkim(String domain, String selector, String expectedPublicKey) {
        String dkimDomain = selector + "._domainkey." + domain;
        List<String> txtRecords = queryDnsTxtRecords(dkimDomain);
        return txtRecords.stream()
            .anyMatch(record -> record.contains(expectedPublicKey));
    }

    private List<String> queryDnsTxtRecords(String domain) {
        Lookup lookup = new Lookup(domain, Type.TXT);
        Record[] records = lookup.run();
        // Extract TXT records
        return Arrays.stream(records)
            .map(r -> ((TXTRecord) r).getStrings())
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
```

---

### Spam Prevention

#### Content Filtering
```java
@Service
public class SpamPreventionService {
    private static final List<String> SPAM_TRIGGER_WORDS = Arrays.asList(
        "free money", "click here", "buy now", "limited time", "act now"
    );

    public SpamScore analyzeContent(String subject, String body) {
        int score = 0;
        String content = (subject + " " + body).toLowerCase();

        // Check spam trigger words
        for (String word : SPAM_TRIGGER_WORDS) {
            if (content.contains(word)) {
                score += 5;
            }
        }

        // Check excessive capitalization
        long capsCount = content.chars().filter(Character::isUpperCase).count();
        if (capsCount > content.length() * 0.3) {
            score += 10;
        }

        // Check link density
        int linkCount = countLinks(body);
        if (linkCount > 10) {
            score += 15;
        }

        return new SpamScore(score, getRecommendations(score));
    }
}
```

#### Bounce Handling
```java
@Service
public class BounceProcessingService {
    public void processBounce(Long contactId, BounceType type) {
        Contact contact = contactRepository.findById(contactId).orElseThrow();

        if (type == BounceType.HARD) {
            // Immediately unsubscribe
            contact.setStatus(ContactStatus.BOUNCED);
            contact.setBounceCount(contact.getBounceCount() + 1);
        } else if (type == BounceType.SOFT) {
            contact.setBounceCount(contact.getBounceCount() + 1);

            // Unsubscribe after 3 soft bounces
            if (contact.getBounceCount() >= 3) {
                contact.setStatus(ContactStatus.BOUNCED);
            }
        }

        contactRepository.save(contact);
    }
}
```

---

### List Hygiene

```java
@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
public void cleanupInactiveContacts() {
    // Remove hard bounces older than 30 days
    LocalDateTime threshold = LocalDateTime.now().minusDays(30);
    List<Contact> hardBounces = contactRepository
        .findByStatusAndLastBounceAtBefore(ContactStatus.BOUNCED, threshold);
    contactRepository.deleteAll(hardBounces);

    // Flag inactive contacts (no opens in 6 months)
    LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
    contactRepository.flagInactiveContacts(sixMonthsAgo);
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)

**Goal:** Set up core infrastructure and security

**Tasks:**
1. Update existing entity models (User, EmailTemplate, EmailCampaign)
2. Create Liquibase migrations (004-019)
3. Run migrations: `mvn liquibase:update`
4. Implement authentication system:
   - `JwtService` for token generation/validation
   - `AuthenticationService` for login/register
   - `UserService` for user management
   - JWT filters in SecurityConfig
5. Create all DTO classes (request/response)
6. Set up global exception handler

**Deliverables:**
- ✅ All database tables created
- ✅ Authentication working (login, register, JWT)
- ✅ DTOs defined
- ✅ Error handling configured

---

### Phase 2: Email Provider Abstraction (Week 3)

**Goal:** Implement multi-provider email sending

**Tasks:**
1. Create `EmailSender` interface
2. Implement providers:
   - `AwsSesProvider`
   - `SendGridProvider`
   - `SmtpProvider`
3. Create `ProviderFactory`
4. Implement `EmailProviderService`
5. Add encryption for credentials
6. Create provider management API endpoints

**Deliverables:**
- ✅ Working email sending via all 3 providers
- ✅ Credentials securely encrypted
- ✅ Provider CRUD endpoints

---

### Phase 3: Domain Management (Week 4)

**Goal:** DNS verification and domain authentication

**Tasks:**
1. Create `Domain` entity and repository
2. Implement `DomainService`
3. Implement `DnsVerificationService` using dnsjava
4. Implement DKIM key generation
5. Create DNS verification scheduler (cron)
6. Create domain management API endpoints

**Deliverables:**
- ✅ Domain CRUD operations
- ✅ DNS record generation (SPF, DKIM, DMARC)
- ✅ Automated verification
- ✅ Domain status tracking

---

### Phase 4: Contact Management (Weeks 5-6)

**Goal:** Complete contact and list management

**Tasks:**
1. Create `Contact`, `ContactList`, `ContactListMembership` entities
2. Implement repositories with custom queries
3. Implement services:
   - `ContactService`
   - `ContactListService`
   - `ContactImportService` (CSV)
   - `ContactExportService` (CSV)
4. Create segment functionality:
   - `Segment` entity
   - `SegmentService` with condition evaluation
5. Implement API endpoints for contacts, lists, segments
6. Add search and filtering

**Deliverables:**
- ✅ Contact CRUD with pagination
- ✅ CSV import/export
- ✅ Contact list management
- ✅ Dynamic segments
- ✅ Tagging system

---

### Phase 5: Email Templates (Week 7)

**Goal:** Template management with WYSIWYG editor

**Tasks:**
1. Update `EmailTemplate` entity
2. Implement `EmailTemplateService`
3. Implement `TemplateRenderer` (variable substitution)
4. Integrate Quill.js editor in frontend
5. Create template API endpoints
6. Add template preview functionality

**Deliverables:**
- ✅ Template CRUD operations
- ✅ Variable substitution ({{first_name}}, etc.)
- ✅ WYSIWYG editor integration
- ✅ Template preview with sample data

---

### Phase 6: Campaign System (Weeks 8-10)

**Goal:** Core campaign functionality

**Tasks:**
1. Update `EmailCampaign` entity
2. Create `CampaignRecipient`, `CampaignLink`, `CampaignClick` entities
3. Implement services:
   - `CampaignService`
   - `CampaignSendingService` (async batch processing)
   - `CampaignSchedulerService` (cron jobs)
   - `TrackingService` (open/click tracking)
4. Implement tracking:
   - Open tracking (1x1 pixel)
   - Click tracking (URL shortening)
5. Create campaign API endpoints
6. Implement webhook handlers for provider events

**Deliverables:**
- ✅ Campaign creation and management
- ✅ Scheduled sending via cron
- ✅ Async batch email sending
- ✅ Open/click tracking
- ✅ Real-time analytics

---

### Phase 7: Analytics & Reporting (Week 11)

**Goal:** Comprehensive analytics and reporting

**Tasks:**
1. Implement `CampaignAnalyticsService`
2. Create analytics aggregation scheduler
3. Build dashboard API endpoints
4. Implement campaign reports with:
   - Open rate, click rate, bounce rate
   - Geographic data
   - Device/client breakdown
5. Export reports to PDF/CSV

**Deliverables:**
- ✅ Real-time campaign statistics
- ✅ Historical analytics
- ✅ Engagement reports
- ✅ Exportable data

---

### Phase 8: Public Subscription API (Week 12)

**Goal:** Website integration and subscription management

**Tasks:**
1. Implement double opt-in flow:
   - Subscribe endpoint
   - Confirmation email
   - Token verification
2. Implement unsubscribe flow
3. Create preference center
4. Implement `ApiKey` management
5. Add rate limiting for public endpoints
6. Create API documentation (Swagger)

**Deliverables:**
- ✅ Public subscription API
- ✅ Double opt-in working
- ✅ Unsubscribe management
- ✅ API key authentication
- ✅ Rate limiting active

---

### Phase 9: Security & Polish (Week 13)

**Goal:** Finalize security and performance

**Tasks:**
1. Implement two-factor authentication
2. Add Redis caching for:
   - Rate limits
   - Segment counts
   - Session storage
3. Implement spam prevention service
4. Add comprehensive logging
5. Performance optimization:
   - Database query optimization
   - Index tuning
   - Caching strategy
6. Security audit

**Deliverables:**
- ✅ 2FA working
- ✅ Redis caching active
- ✅ Spam prevention enabled
- ✅ Optimized queries
- ✅ Security hardened

---

### Phase 10: Frontend & Testing (Weeks 14-15)

**Goal:** Complete UI and testing

**Tasks:**
1. Create Thymeleaf templates:
   - Dashboard
   - Contact management UI
   - Campaign builder
   - Analytics dashboard
2. Implement frontend validation
3. Write comprehensive tests:
   - Unit tests (80%+ coverage)
   - Integration tests
   - End-to-end tests
4. Load testing
5. Documentation

**Deliverables:**
- ✅ Complete web interface
- ✅ Test coverage >80%
- ✅ Load tested (10K concurrent users)
- ✅ Production-ready

---

## Dependencies

### Required Maven Dependencies

Add to `pom.xml`:

```xml
<dependencies>
    <!-- Existing dependencies -->

    <!-- JWT Authentication -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- AWS SES -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>ses</artifactId>
        <version>2.21.0</version>
    </dependency>

    <!-- SendGrid -->
    <dependency>
        <groupId>com.sendgrid</groupId>
        <artifactId>sendgrid-java</artifactId>
        <version>4.10.2</version>
    </dependency>

    <!-- CSV Processing -->
    <dependency>
        <groupId>com.opencsv</groupId>
        <artifactId>opencsv</artifactId>
        <version>5.9</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- DNS Lookup -->
    <dependency>
        <groupId>dnsjava</groupId>
        <artifactId>dnsjava</artifactId>
        <version>3.5.3</version>
    </dependency>

    <!-- DKIM Signing -->
    <dependency>
        <groupId>net.markenwerk</groupId>
        <artifactId>utils-mail-dkim</artifactId>
        <version>2.0.0</version>
    </dependency>

    <!-- Email Validation -->
    <dependency>
        <groupId>commons-validator</groupId>
        <artifactId>commons-validator</artifactId>
        <version>1.8.0</version>
    </dependency>

    <!-- Two-Factor Authentication -->
    <dependency>
        <groupId>dev.samstevens.totp</groupId>
        <artifactId>totp</artifactId>
        <version>1.7.1</version>
    </dependency>

    <!-- MapStruct for DTO mapping -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## Scalability & Performance

### Database Optimization

**Connection Pooling (HikariCP):**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Query Optimization:**
- Use batch inserts for bulk operations
- Implement pagination for all list endpoints
- Use `@EntityGraph` to prevent N+1 queries
- Index foreign keys and frequently queried columns

---

### Caching Strategy

**Redis Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**What to Cache:**
- User sessions (15 min TTL)
- Segment counts (10 min TTL)
- Domain verification status (1 hour TTL)
- Rate limit counters (window duration TTL)
- Campaign statistics (5 min TTL)

---

### Async Processing

**Thread Pool Configuration:**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**Use Cases:**
- Campaign sending (batch processing)
- CSV import (large files)
- Webhook processing
- Analytics aggregation
- Email delivery

---

## Monitoring & Operations

### Logging Strategy

**Structured Logging:**
```java
@Slf4j
@Service
public class CampaignService {
    public void sendCampaign(Long campaignId) {
        log.info("Starting campaign send",
                 kv("campaignId", campaignId),
                 kv("userId", getCurrentUserId()));

        try {
            // Send logic
            log.info("Campaign sent successfully",
                     kv("campaignId", campaignId),
                     kv("recipientCount", count));
        } catch (Exception e) {
            log.error("Campaign send failed",
                      kv("campaignId", campaignId),
                      kv("error", e.getMessage()));
        }
    }
}
```

---

### Metrics to Track

**Application Metrics:**
- API request rate (requests/second)
- API response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Campaign send rate (emails/minute)
- Email delivery rate (%)
- Bounce rate (%)
- Database query time

**Business Metrics:**
- Total users
- Active campaigns
- Emails sent (daily, monthly)
- Contact growth rate
- Revenue (if applicable)

---

### Alerting Rules

**Critical Alerts:**
- High bounce rate (>5%)
- High complaint rate (>0.1%)
- Provider API failures
- Database connection failures
- Disk space <10%

**Warning Alerts:**
- API response time >500ms
- Campaign stuck in SENDING >1 hour
- Rate limit hits >1000/hour
- Failed login attempts >100/hour

---

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {
    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void testCreateContact() {
        ContactCreateRequest request = new ContactCreateRequest(
            "test@example.com", "Test", "User", null, null, true, null
        );

        Contact saved = contactService.create(1L, request);

        assertNotNull(saved.getId());
        assertEquals("test@example.com", saved.getEmail());
        verify(contactRepository).save(any(Contact.class));
    }
}
```

---

### Integration Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ContactControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCreateContactEndpoint() {
        ContactCreateRequest request = new ContactCreateRequest(...);

        ResponseEntity<ApiResponse> response = restTemplate
            .postForEntity("/api/v1/contacts", request, ApiResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }
}
```

---

### End-to-End Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CampaignE2ETest {
    @Test
    void testCompleteCampaignFlow() {
        // 1. Login
        String token = login("user@example.com", "password");

        // 2. Create contact list
        Long listId = createContactList(token);

        // 3. Import contacts
        importContacts(token, listId, "contacts.csv");

        // 4. Create template
        Long templateId = createTemplate(token);

        // 5. Create campaign
        Long campaignId = createCampaign(token, listId, templateId);

        // 6. Send campaign
        sendCampaign(token, campaignId);

        // 7. Verify campaign status
        CampaignStats stats = getCampaignStats(token, campaignId);
        assertEquals("SENT", stats.getStatus());
        assertTrue(stats.getSentCount() > 0);
    }
}
```

---

## Summary

OpenMailer provides a complete, production-ready email marketing platform with:

✅ **Robust API** - 50+ REST endpoints
✅ **Multi-Provider Support** - AWS SES, SendGrid, SMTP
✅ **DNS Verification** - SPF, DKIM, DMARC
✅ **Advanced Contact Management** - Lists, segments, tags, CSV
✅ **Campaign System** - Scheduling, tracking, analytics
✅ **Enterprise Security** - JWT, API keys, rate limiting, encryption
✅ **GDPR Compliant** - Consent tracking, data export, deletion
✅ **Scalable Architecture** - Redis caching, async processing
✅ **Deliverability Focus** - Spam prevention, bounce handling

Ready for implementation following the 15-week roadmap!

---

**Related Documentation:**
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture and design
- [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) - Complete database schema
