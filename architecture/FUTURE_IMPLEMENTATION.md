# Future Implementation - Advanced Technologies

**OpenMailer - High-Impact Technical Enhancements**

Last Updated: 2024-12-24

---

## 🎯 Purpose

This document outlines advanced technologies and patterns that can be implemented in OpenMailer to:
- Demonstrate industry-standard expertise
- Improve scalability and performance
- Increase hiring appeal (specialized skills)
- Enhance production readiness

**Key Criteria:**
- ✅ Free or low-cost to implement
- ✅ High demand in job market
- ✅ Relevant to email marketing domain
- ✅ Demonstrates advanced engineering skills

---

## 📊 Implementation Priority Matrix

| Technology | Impact | Complexity | Time | Interview Value | Status |
|-----------|--------|------------|------|----------------|--------|
| Redis Advanced Patterns | HIGH | MEDIUM | 2-3 days | ⭐⭐⭐⭐⭐ | ⏳ Planned |
| Elasticsearch | VERY HIGH | MEDIUM | 3-4 days | ⭐⭐⭐⭐⭐ | ⏳ Planned |
| Prometheus + Grafana | HIGH | LOW | 1 day | ⭐⭐⭐⭐ | ⏳ Planned |
| Webhook Delivery System | MEDIUM | MEDIUM | 2-3 days | ⭐⭐⭐⭐ | ⏳ Planned |
| gRPC Microservices | HIGH | HIGH | 4-5 days | ⭐⭐⭐⭐⭐ | ⏳ Future |
| Custom Query Language | VERY HIGH | HIGH | 5-7 days | ⭐⭐⭐⭐⭐ | ⏳ Future |
| Database Sharding | HIGH | HIGH | 3-4 days | ⭐⭐⭐⭐ | ⏳ Future |
| Apache Kafka | VERY HIGH | MEDIUM | 3-4 days | ⭐⭐⭐⭐⭐ | 💰 Expensive |

---

## 🔥 Tier 1: High Priority (Implement First)

### 1. Elasticsearch - Full-Text Search & Analytics

**Status:** ⏳ NOT STARTED
**Priority:** VERY HIGH
**Estimated Time:** 3-4 days
**Cost:** FREE (Docker container)

#### Why Elasticsearch?

- Used by: GitHub, Uber, Netflix, Shopify, Stripe
- High-demand skill with salary premium
- Perfect for email marketing analytics
- Solves real performance problems

#### What to Implement

**A. Contact Search**
```
Features:
- Full-text search: "Find all contacts in New York who opened Black Friday campaign"
- Fuzzy matching: "john@gmal.com" → suggests "john@gmail.com"
- Autocomplete for email addresses
- Filter by tags, status, engagement level
- Search 10M+ contacts in <100ms
```

**B. Campaign Analytics**
```
Aggregations:
- Open rates by hour/day/week/month
- Click heatmaps by geography
- Engagement trends over time
- Top performing subject lines
- Device/browser statistics
- Real-time dashboard queries
```

**C. Email Event Search**
```
Query examples:
- "Show all bounced emails from @yahoo.com in last 7 days"
- "Find campaigns with >20% open rate sent on weekends"
- "Search email content for 'discount' with click rate >5%"
```

#### Technical Implementation

**Data Model:**
```json
// Index: contacts
{
  "contact_id": 12345,
  "email": "john@example.com",
  "name": "John Doe",
  "status": "subscribed",
  "tags": ["premium", "early-adopter"],
  "custom_fields": {
    "company": "Acme Corp",
    "role": "Developer"
  },
  "engagement_score": 8.5,
  "last_activity": "2024-12-20T10:30:00Z",
  "total_opens": 45,
  "total_clicks": 23
}

// Index: email_events
{
  "event_id": "evt_123",
  "event_type": "opened",
  "campaign_id": 789,
  "contact_id": 12345,
  "timestamp": "2024-12-20T10:30:00Z",
  "metadata": {
    "user_agent": "Mozilla/5.0...",
    "ip_address": "192.168.1.1",
    "location": "San Francisco, CA",
    "device": "mobile"
  }
}

// Index: campaigns
{
  "campaign_id": 789,
  "name": "Black Friday Sale",
  "subject": "50% Off Everything!",
  "sent_at": "2024-11-29T08:00:00Z",
  "stats": {
    "sent": 10000,
    "opened": 3500,
    "clicked": 850,
    "bounced": 45
  }
}
```

**Spring Integration:**
```java
@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        return new RestHighLevelClient(
            RestClient.builder(
                new HttpHost("localhost", 9200, "http")
            )
        );
    }
}

@Service
public class ContactSearchService {

    private final RestHighLevelClient client;

    public SearchResult searchContacts(String query, List<String> tags) {
        SearchRequest request = new SearchRequest("contacts");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Full-text search
        if (query != null) {
            boolQuery.must(QueryBuilders.multiMatchQuery(query)
                .field("email", 2.0f)  // Boost email matches
                .field("name")
                .field("custom_fields.*")
                .fuzziness(Fuzziness.AUTO));
        }

        // Filter by tags
        if (tags != null && !tags.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", tags));
        }

        builder.query(boolQuery);
        builder.size(100);

        request.source(builder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return parseResponse(response);
    }
}

@Service
public class CampaignAnalyticsService {

    public Map<String, Object> getEngagementTimeline(Long campaignId) {
        SearchRequest request = new SearchRequest("email_events");
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // Filter by campaign
        builder.query(QueryBuilders.termQuery("campaign_id", campaignId));

        // Aggregate by hour
        DateHistogramAggregationBuilder agg = AggregationBuilders
            .dateHistogram("events_over_time")
            .field("timestamp")
            .calendarInterval(DateHistogramInterval.HOUR)
            .subAggregation(
                AggregationBuilders.terms("event_types")
                    .field("event_type")
            );

        builder.aggregation(agg);
        builder.size(0);  // Don't need docs, only aggregations

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return parseAggregations(response);
    }
}
```

#### Interview Talking Points

- **Inverted Indexes:** Explain how ES indexes data for fast full-text search
- **Mapping Design:** Analyzed vs keyword fields, multi-fields
- **Aggregations:** Bucket aggregations (terms, date histogram) vs metric aggregations
- **Relevance Scoring:** BM25 algorithm, boosting, function scores
- **Scaling:** Shards, replicas, cluster management
- **Performance:** Query optimization, filter context vs query context

**Example Answer:**
> "I used Elasticsearch for campaign analytics because MySQL was too slow for complex aggregations across millions of email events. With ES, I can run queries like 'show hourly open rates by country for the last 30 days' in under 200ms vs 10+ seconds in PostgreSQL. I designed the mapping with analyzed fields for full-text search and keyword fields for exact filtering."

#### Dependencies

```xml
<!-- Elasticsearch -->
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.17.15</version>
</dependency>

<!-- Or use Spring Data Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

#### Docker Setup

```yaml
# docker-compose.yml
version: '3.8'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.15
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:7.17.15
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

volumes:
  es_data:
```

**Start:** `docker-compose up -d`

---

### 2. Redis Advanced Patterns

**Status:** ⏳ NOT STARTED (Basic caching planned in Phase 9)
**Priority:** VERY HIGH
**Estimated Time:** 2-3 days
**Cost:** FREE

#### Why Advanced Redis?

- Every major tech company uses Redis heavily
- Shows distributed systems knowledge
- Goes beyond basic caching (differentiates you)
- Solves real concurrency problems

#### What to Implement

**A. Distributed Locking**

Prevents duplicate campaign sends across multiple servers.

```java
@Service
public class CampaignLockService {

    private final StringRedisTemplate redisTemplate;

    public boolean sendCampaignWithLock(Long campaignId) {
        String lockKey = "campaign:lock:" + campaignId;
        String lockValue = UUID.randomUUID().toString();

        // Try to acquire lock (5 minute expiry)
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, 5, TimeUnit.MINUTES);

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // Only ONE server can execute this
                campaignSendingService.executeCampaign(campaignId);
                return true;
            } finally {
                // Release lock only if we still own it
                releaseLock(lockKey, lockValue);
            }
        }

        return false;  // Another server is processing
    }

    private void releaseLock(String key, String value) {
        // Lua script for atomic check-and-delete
        String script =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

        redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            value
        );
    }
}
```

**B. Rate Limiting with Sliding Window**

Precise rate limiting using Lua scripts.

```java
@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean allowRequest(String userId, int maxRequests, int windowSeconds) {
        String key = "rate_limit:" + userId;

        // Lua script for atomic sliding window rate limiting
        String luaScript = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])

            -- Remove old entries outside the window
            redis.call('ZREMRANGEBYSCORE', key, 0, current_time - window)

            -- Count current requests
            local current_count = redis.call('ZCARD', key)

            if current_count < limit then
                -- Add new request
                redis.call('ZADD', key, current_time, current_time)
                redis.call('EXPIRE', key, window)
                return 1  -- Allow
            else
                return 0  -- Deny
            end
        """;

        Long allowed = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(maxRequests),
            String.valueOf(windowSeconds),
            String.valueOf(System.currentTimeMillis())
        );

        return allowed != null && allowed == 1;
    }
}

// Usage in controller
@RestController
public class ApiController {

    @GetMapping("/api/v1/campaigns")
    public ResponseEntity<?> getCampaigns(@AuthenticationPrincipal User user) {
        // Limit to 100 requests per minute
        if (!rateLimitService.allowRequest(user.getId(), 100, 60)) {
            return ResponseEntity.status(429)
                .body("Rate limit exceeded. Try again later.");
        }

        return ResponseEntity.ok(campaignService.getCampaigns());
    }
}
```

**C. Pub/Sub for Real-Time Updates**

Broadcast campaign events to connected dashboard users.

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public MessageListenerAdapter campaignEventListener(
            CampaignEventHandler handler) {
        return new MessageListenerAdapter(handler, "handleMessage");
    }
}

@Service
public class CampaignEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishEmailOpened(Long campaignId, String email, String location) {
        Map<String, Object> event = Map.of(
            "type", "email_opened",
            "campaign_id", campaignId,
            "email", email,
            "location", location,
            "timestamp", System.currentTimeMillis()
        );

        String channel = "campaign:" + campaignId + ":events";
        redisTemplate.convertAndSend(channel, event);
    }
}

@Component
public class CampaignEventHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public void handleMessage(Map<String, Object> event) {
        Long campaignId = (Long) event.get("campaign_id");

        // Forward to WebSocket subscribers
        messagingTemplate.convertAndSend(
            "/topic/campaign/" + campaignId,
            event
        );
    }
}
```

**D. Leaderboard with Sorted Sets**

Track top-performing campaigns.

```java
@Service
public class CampaignLeaderboardService {

    private final RedisTemplate<String, String> redisTemplate;

    public void updateCampaignScore(Long campaignId, double openRate) {
        String key = "leaderboard:campaigns:open_rate";
        redisTemplate.opsForZSet().add(key, campaignId.toString(), openRate);
    }

    public List<CampaignScore> getTopCampaigns(int limit) {
        String key = "leaderboard:campaigns:open_rate";

        // Get top N campaigns with scores
        Set<ZSetOperations.TypedTuple<String>> results =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        return results.stream()
            .map(tuple -> new CampaignScore(
                Long.parseLong(tuple.getValue()),
                tuple.getScore()
            ))
            .collect(Collectors.toList());
    }

    public Long getCampaignRank(Long campaignId) {
        String key = "leaderboard:campaigns:open_rate";
        return redisTemplate.opsForZSet()
            .reverseRank(key, campaignId.toString());
    }
}
```

**E. Session Storage with TTL**

Store temporary data with automatic expiration.

```java
@Service
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void storeConfirmationToken(String token, Long contactId) {
        String key = "confirmation:" + token;

        // Store for 24 hours
        redisTemplate.opsForValue().set(
            key,
            contactId,
            24,
            TimeUnit.HOURS
        );
    }

    public void storeImportJob(String jobId, ImportJob job) {
        String key = "import_job:" + jobId;

        // Complex objects stored as JSON
        redisTemplate.opsForValue().set(
            key,
            job,
            7,
            TimeUnit.DAYS
        );
    }

    public void incrementCampaignProgress(Long campaignId) {
        String key = "campaign:progress:" + campaignId;
        redisTemplate.opsForValue().increment(key);
    }
}
```

#### Interview Talking Points

- **Atomic Operations:** Why Lua scripts are necessary
- **Distributed Locking:** Challenges (lock expiry, deadlocks, thundering herd)
- **Data Structures:** When to use strings vs hashes vs sorted sets
- **TTL Strategies:** Memory management, eviction policies
- **Pub/Sub vs Streams:** Trade-offs and use cases

**Example Answer:**
> "I implemented distributed locking with Redis to prevent duplicate campaign sends when running multiple application servers. Using Lua scripts ensures atomicity - the check-and-set operation can't be interrupted. I also built a sliding window rate limiter that's more accurate than fixed windows, preventing burst traffic from overwhelming our email providers."

---

### 3. Prometheus + Grafana Metrics

**Status:** ⏳ NOT STARTED
**Priority:** HIGH
**Estimated Time:** 1 day
**Cost:** FREE

#### Why Prometheus?

- Industry standard for metrics (CNCF project)
- Shows DevOps/SRE mindset
- Production-ready monitoring
- Beautiful dashboards with Grafana

#### What to Implement

**Metrics to Track:**

1. **Email Metrics**
   - Total emails sent (counter)
   - Email send duration (histogram)
   - Emails per second (gauge)
   - Emails by provider (counter with labels)

2. **Campaign Metrics**
   - Active campaigns (gauge)
   - Campaign send rate (counter)
   - Campaign completion time (histogram)

3. **System Metrics**
   - JVM memory usage
   - Database connection pool
   - Redis cache hit/miss rate
   - HTTP request latency

4. **Business Metrics**
   - Open rate (gauge)
   - Click-through rate (gauge)
   - Bounce rate (gauge)
   - Active subscribers (gauge)

#### Implementation

```java
@Service
public class CampaignSendingService {

    private final Counter emailsSent;
    private final Timer sendDuration;
    private final Gauge activeCampaigns;
    private final Counter sendErrors;

    public CampaignSendingService(MeterRegistry registry) {
        this.emailsSent = Counter.builder("openmailer_emails_sent_total")
            .description("Total number of emails sent")
            .tag("app", "openmailer")
            .register(registry);

        this.sendDuration = Timer.builder("openmailer_email_send_duration_seconds")
            .description("Time taken to send email")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);

        this.activeCampaigns = Gauge.builder("openmailer_active_campaigns",
                this::getActiveCampaignCount)
            .description("Number of campaigns currently sending")
            .register(registry);

        this.sendErrors = Counter.builder("openmailer_send_errors_total")
            .description("Total email send errors")
            .register(registry);
    }

    public void sendEmail(EmailSendRequest request, String provider) {
        Timer.Sample sample = Timer.start();

        try {
            emailProvider.send(request);

            emailsSent.increment();

            // Track by provider
            Counter.builder("openmailer_emails_by_provider")
                .tag("provider", provider)
                .register(meterRegistry)
                .increment();

        } catch (Exception e) {
            sendErrors.increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("openmailer_email_send_duration")
                .tag("provider", provider)
                .register(meterRegistry));
        }
    }
}

@RestController
public class CampaignController {

    // Automatically track HTTP metrics
    @Timed(value = "openmailer_http_requests",
           percentiles = {0.5, 0.95, 0.99})
    @GetMapping("/api/v1/campaigns/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaign(id));
    }
}

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "openmailer",
                "environment", environment
            );
    }
}
```

#### Grafana Dashboard Queries

```promql
# Emails sent per second (rate)
rate(openmailer_emails_sent_total[5m])

# P95 email send latency
histogram_quantile(0.95, rate(openmailer_email_send_duration_seconds_bucket[5m]))

# Error rate percentage
(rate(openmailer_send_errors_total[5m]) / rate(openmailer_emails_sent_total[5m])) * 100

# Emails by provider
sum by (provider) (rate(openmailer_emails_by_provider[5m]))

# Active campaigns
openmailer_active_campaigns

# HTTP request rate
rate(openmailer_http_requests_seconds_count[5m])

# HTTP P99 latency
histogram_quantile(0.99, rate(openmailer_http_requests_seconds_bucket[5m]))
```

#### Dependencies

```xml
<!-- Micrometer Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Configuration

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,prometheus
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

#### Docker Setup

```yaml
# docker-compose.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  prometheus_data:
  grafana_data:
```

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'openmailer'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

**Access Grafana:** http://localhost:3000 (admin/admin)

#### Interview Talking Points

- **RED Method:** Rate, Errors, Duration for monitoring services
- **USE Method:** Utilization, Saturation, Errors for resources
- **Cardinality:** Why too many unique label values cause problems
- **Percentiles vs Averages:** Why P95/P99 matter more than average
- **Alerting:** Prometheus AlertManager integration

---

### 4. Webhook Delivery System with Retries

**Status:** ⏳ NOT STARTED (Basic webhooks exist in Phase 6)
**Priority:** HIGH
**Estimated Time:** 2-3 days
**Cost:** FREE

#### Why Webhook System?

- Every SaaS platform needs reliable webhooks
- Shows async/reliability expertise
- Real-world integration pattern
- Companies like Stripe/Shopify heavily rely on this

#### What to Implement

**Problem:** Customer's server is down when you try to deliver webhook. How do you handle it?

**Solution:**
1. Exponential backoff retries
2. Dead letter queue for failures
3. Webhook signature verification
4. Delivery status dashboard
5. Manual retry capability

#### Implementation

```java
@Entity
public class WebhookSubscription {
    @Id
    private Long id;

    private Long userId;
    private String url;
    private String secret;  // For HMAC signatures
    private List<String> events;  // ["email.sent", "email.opened"]
    private boolean active;

    private int maxRetries = 5;
    private int timeoutSeconds = 30;
}

@Entity
public class WebhookDelivery {
    @Id
    private Long id;

    private Long subscriptionId;
    private String eventType;
    private String payload;

    private DeliveryStatus status;  // PENDING, SENT, FAILED, DEAD_LETTER
    private int attemptCount;
    private LocalDateTime nextRetryAt;

    private Integer responseCode;
    private String responseBody;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

@Service
public class WebhookDeliveryService {

    private final RestTemplate restTemplate;
    private final WebhookDeliveryRepository deliveryRepo;

    @Async
    public void deliver(WebhookEvent event) {
        List<WebhookSubscription> subscriptions = findSubscriptions(event.getType());

        for (WebhookSubscription subscription : subscriptions) {
            WebhookDelivery delivery = createDelivery(subscription, event);
            deliveryRepo.save(delivery);

            deliverWithRetry(delivery);
        }
    }

    private void deliverWithRetry(WebhookDelivery delivery) {
        WebhookSubscription subscription = getSubscription(delivery);

        RetryPolicy<HttpResponse> retryPolicy = RetryPolicy.<HttpResponse>builder()
            .handle(IOException.class, HttpException.class)
            .handleResultIf(response -> response.getStatusCode() >= 500)
            .withBackoff(
                Duration.ofSeconds(1),
                Duration.ofMinutes(30),
                2.0  // Exponential: 1s, 2s, 4s, 8s, 16s, 30s
            )
            .withMaxRetries(subscription.getMaxRetries())
            .onRetry(e -> {
                delivery.setAttemptCount(e.getAttemptCount());
                delivery.setNextRetryAt(LocalDateTime.now().plus(
                    calculateBackoff(e.getAttemptCount())
                ));
                deliveryRepo.save(delivery);

                log.info("Webhook retry {}/{} for delivery {}",
                    e.getAttemptCount(),
                    subscription.getMaxRetries(),
                    delivery.getId());
            })
            .onSuccess(e -> {
                delivery.setStatus(DeliveryStatus.SENT);
                delivery.setResponseCode(e.getResult().getStatusCode());
                delivery.setCompletedAt(LocalDateTime.now());
                deliveryRepo.save(delivery);
            })
            .onFailure(e -> {
                delivery.setStatus(DeliveryStatus.DEAD_LETTER);
                delivery.setErrorMessage(e.getException().getMessage());
                delivery.setCompletedAt(LocalDateTime.now());
                deliveryRepo.save(delivery);

                // Notify admin about failed webhook
                notifyWebhookFailure(delivery);
            })
            .build();

        Failsafe.with(retryPolicy).run(() -> sendHttpRequest(subscription, delivery));
    }

    private HttpResponse sendHttpRequest(WebhookSubscription sub, WebhookDelivery delivery) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add HMAC signature for verification
        String signature = generateHmacSignature(delivery.getPayload(), sub.getSecret());
        headers.set("X-Webhook-Signature", signature);
        headers.set("X-Webhook-ID", delivery.getId().toString());
        headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));

        HttpEntity<String> request = new HttpEntity<>(delivery.getPayload(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            sub.getUrl(),
            request,
            String.class
        );

        return new HttpResponse(
            response.getStatusCodeValue(),
            response.getBody()
        );
    }

    private String generateHmacSignature(String payload, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    private Duration calculateBackoff(int attempt) {
        // Exponential: 1s, 2s, 4s, 8s, 16s, 32s
        long seconds = (long) Math.pow(2, attempt - 1);
        return Duration.ofSeconds(Math.min(seconds, 1800)); // Max 30 min
    }
}

// Scheduled job to retry failed webhooks
@Service
public class WebhookRetryScheduler {

    @Scheduled(fixedDelay = 60000)  // Every minute
    public void retryPendingWebhooks() {
        List<WebhookDelivery> pending = deliveryRepo.findPendingRetries(
            LocalDateTime.now()
        );

        for (WebhookDelivery delivery : pending) {
            webhookDeliveryService.deliverWithRetry(delivery);
        }
    }
}

// Controller for managing webhooks
@RestController
@RequestMapping("/api/v1/webhooks/subscriptions")
public class WebhookSubscriptionController {

    @PostMapping
    public ResponseEntity<WebhookSubscription> subscribe(
            @RequestBody WebhookSubscriptionRequest request) {

        // Verify URL is reachable
        boolean verified = webhookService.verifyEndpoint(request.getUrl());
        if (!verified) {
            return ResponseEntity.badRequest()
                .body("Could not verify webhook URL");
        }

        WebhookSubscription sub = webhookService.createSubscription(request);
        return ResponseEntity.ok(sub);
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<Page<WebhookDelivery>> getDeliveries(
            @PathVariable Long id,
            Pageable pageable) {

        Page<WebhookDelivery> deliveries = deliveryRepo
            .findBySubscriptionId(id, pageable);

        return ResponseEntity.ok(deliveries);
    }

    @PostMapping("/deliveries/{id}/retry")
    public ResponseEntity<Void> retryDelivery(@PathVariable Long id) {
        WebhookDelivery delivery = deliveryRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Delivery not found"));

        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextRetryAt(LocalDateTime.now());
        deliveryRepo.save(delivery);

        return ResponseEntity.ok().build();
    }
}
```

#### Webhook Events

```json
// Event types to support
{
  "email.sent": {
    "campaign_id": 123,
    "contact_email": "user@example.com",
    "sent_at": "2024-12-20T10:30:00Z"
  },

  "email.opened": {
    "campaign_id": 123,
    "contact_email": "user@example.com",
    "opened_at": "2024-12-20T10:35:00Z",
    "location": "San Francisco, CA",
    "device": "mobile"
  },

  "email.clicked": {
    "campaign_id": 123,
    "contact_email": "user@example.com",
    "link_url": "https://example.com/promo",
    "clicked_at": "2024-12-20T10:36:00Z"
  },

  "email.bounced": {
    "campaign_id": 123,
    "contact_email": "user@example.com",
    "bounce_type": "hard",
    "reason": "Mailbox does not exist"
  },

  "contact.subscribed": {
    "contact_id": 456,
    "email": "newuser@example.com",
    "list_id": 789
  },

  "contact.unsubscribed": {
    "contact_id": 456,
    "email": "user@example.com",
    "reason": "No longer interested"
  }
}
```

#### Dependencies

```xml
<!-- Resilience4j for retries -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Or use Failsafe -->
<dependency>
    <groupId>dev.failsafe</groupId>
    <artifactId>failsafe</artifactId>
    <version>3.3.2</version>
</dependency>

<!-- Apache Commons Codec for HMAC -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
</dependency>
```

#### Interview Talking Points

- **Idempotency:** Why webhooks must be idempotent
- **Ordering:** Can webhooks arrive out of order?
- **Security:** HMAC signatures, replay attack prevention
- **Retry Strategies:** Exponential backoff vs linear
- **Dead Letter Queues:** When to give up
- **At-least-once delivery:** Why exactly-once is nearly impossible

**Example Answer:**
> "I built a robust webhook delivery system with exponential backoff retries and dead letter queue handling. Each webhook includes an HMAC signature so receivers can verify authenticity. If a customer's endpoint is down, we retry with increasing delays (1s, 2s, 4s...) up to 5 times before moving to a dead letter queue where admins can manually retry. This pattern is similar to how Stripe handles webhooks."

---

## 🎯 Tier 2: Advanced (Implement Later)

### 5. gRPC for Internal Microservices

**Status:** ⏳ NOT STARTED
**Priority:** HIGH
**Estimated Time:** 4-5 days
**Cost:** FREE

#### Why gRPC?

- Google's standard (YouTube, Google Cloud, Kubernetes)
- HTTP/2 based - faster than REST
- Strong typing with Protocol Buffers
- Bi-directional streaming
- Shows microservices expertise

#### When to Use

Currently, OpenMailer is a monolith. gRPC makes sense when you split into services:

```
┌──────────────┐      gRPC      ┌──────────────────┐
│  Campaign    │ ───────────────▶│  Email Sending   │
│  API Service │                 │  Service         │
│  (REST)      │                 │  (gRPC)          │
└──────────────┘                 └──────────────────┘
       │                                  │
       │                                  │
       │          gRPC                    │
       └──────────────────▶┌──────────────────────┐
                           │  Analytics Service   │
                           │  (gRPC)              │
                           └──────────────────────┘
```

**Services to Extract:**
1. **Email Sending Service** - Heavy processing, scales independently
2. **Analytics Service** - Read-heavy, can have separate database
3. **Template Rendering Service** - CPU intensive

#### Implementation

**Step 1: Define Protocol Buffers**

```protobuf
// analytics.proto
syntax = "proto3";

package openmailer.analytics;

option java_package = "com.openmailer.grpc.analytics";
option java_multiple_files = true;

service AnalyticsService {
  // Get campaign statistics
  rpc GetCampaignStats(CampaignStatsRequest) returns (CampaignStats);

  // Get real-time metrics (streaming)
  rpc StreamMetrics(stream MetricRequest) returns (stream Metric);

  // Batch get multiple campaign stats
  rpc BatchGetStats(BatchStatsRequest) returns (stream CampaignStats);
}

message CampaignStatsRequest {
  int64 campaign_id = 1;
  bool include_demographics = 2;
  bool include_timeline = 3;
}

message CampaignStats {
  int64 campaign_id = 1;
  string campaign_name = 2;

  int32 sent_count = 3;
  int32 opened_count = 4;
  int32 clicked_count = 5;
  int32 bounced_count = 6;

  double open_rate = 7;
  double click_rate = 8;
  double bounce_rate = 9;

  Demographics demographics = 10;
  repeated TimelinePoint timeline = 11;
}

message Demographics {
  map<string, int32> by_country = 1;
  map<string, int32> by_device = 2;
  map<string, int32> by_email_client = 3;
}

message TimelinePoint {
  int64 timestamp = 1;
  int32 opens = 2;
  int32 clicks = 3;
}

message MetricRequest {
  repeated int64 campaign_ids = 1;
}

message Metric {
  int64 campaign_id = 1;
  string metric_name = 2;
  double value = 3;
  int64 timestamp = 4;
}

message BatchStatsRequest {
  repeated int64 campaign_ids = 1;
}
```

```protobuf
// email_sending.proto
syntax = "proto3";

package openmailer.sending;

option java_package = "com.openmailer.grpc.sending";
option java_multiple_files = true;

service EmailSendingService {
  rpc SendEmail(SendEmailRequest) returns (SendEmailResponse);
  rpc SendBatch(stream SendEmailRequest) returns (stream SendEmailResponse);
}

message SendEmailRequest {
  string to = 1;
  string from = 2;
  string subject = 3;
  string html_body = 4;
  string text_body = 5;

  repeated string cc = 6;
  repeated string bcc = 7;
  string reply_to = 8;

  repeated Attachment attachments = 9;

  string provider = 10;  // "sendgrid", "aws-ses", "smtp"

  map<string, string> headers = 11;
}

message Attachment {
  string filename = 1;
  bytes content = 2;
  string content_type = 3;
}

message SendEmailResponse {
  bool success = 1;
  string message_id = 2;
  string error_message = 3;
  int64 timestamp = 4;
}
```

**Step 2: Generate Java Code**

Add to `pom.xml`:

```xml
<dependencies>
    <!-- gRPC -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.60.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>1.60.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>1.60.0</version>
    </dependency>
    <dependency>
        <groupId>javax.annotation</groupId>
        <artifactId>javax.annotation-api</artifactId>
        <version>1.3.2</version>
    </dependency>
</dependencies>

<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.25.1:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.60.0:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Step 3: Implement gRPC Server**

```java
@Service
public class AnalyticsGrpcService extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {

    private final CampaignAnalyticsService analyticsService;

    @Override
    public void getCampaignStats(
            CampaignStatsRequest request,
            StreamObserver<CampaignStats> responseObserver) {

        try {
            Long campaignId = request.getCampaignId();

            // Fetch from database
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

            // Build response
            CampaignStats.Builder builder = CampaignStats.newBuilder()
                .setCampaignId(campaignId)
                .setCampaignName(campaign.getName())
                .setSentCount(campaign.getSentCount())
                .setOpenedCount(campaign.getOpenedCount())
                .setClickedCount(campaign.getClickedCount())
                .setBouncedCount(campaign.getBouncedCount())
                .setOpenRate(campaign.getOpenRate())
                .setClickRate(campaign.getClickRate())
                .setBounceRate(campaign.getBounceRate());

            // Include demographics if requested
            if (request.getIncludeDemographics()) {
                Demographics demographics = buildDemographics(campaignId);
                builder.setDemographics(demographics);
            }

            // Include timeline if requested
            if (request.getIncludeTimeline()) {
                List<TimelinePoint> timeline = buildTimeline(campaignId);
                builder.addAllTimeline(timeline);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public StreamObserver<MetricRequest> streamMetrics(
            StreamObserver<Metric> responseObserver) {

        return new StreamObserver<MetricRequest>() {
            @Override
            public void onNext(MetricRequest request) {
                // Client sends campaign IDs
                for (Long campaignId : request.getCampaignIdsList()) {
                    // Stream back real-time metrics
                    Metric metric = Metric.newBuilder()
                        .setCampaignId(campaignId)
                        .setMetricName("open_rate")
                        .setValue(getCurrentOpenRate(campaignId))
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                    responseObserver.onNext(metric);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in metrics stream", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void batchGetStats(
            BatchStatsRequest request,
            StreamObserver<CampaignStats> responseObserver) {

        // Stream results as they're computed (good for large batches)
        for (Long campaignId : request.getCampaignIdsList()) {
            CampaignStatsRequest statsRequest = CampaignStatsRequest.newBuilder()
                .setCampaignId(campaignId)
                .build();

            getCampaignStats(statsRequest, new StreamObserver<CampaignStats>() {
                @Override
                public void onNext(CampaignStats stats) {
                    responseObserver.onNext(stats);
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Error fetching stats for campaign " + campaignId, t);
                }

                @Override
                public void onCompleted() {
                    // Don't complete the main stream yet
                }
            });
        }

        responseObserver.onCompleted();
    }
}

@Configuration
public class GrpcServerConfig {

    @Bean
    public Server grpcServer(AnalyticsGrpcService analyticsService) throws IOException {
        return ServerBuilder.forPort(9090)
            .addService(analyticsService)
            .build()
            .start();
    }

    @PreDestroy
    public void shutdownGrpcServer() {
        if (grpcServer != null) {
            grpcServer.shutdown();
        }
    }
}
```

**Step 4: Implement gRPC Client**

```java
@Service
public class AnalyticsGrpcClient {

    private final AnalyticsServiceBlockingStub blockingStub;
    private final AnalyticsServiceStub asyncStub;

    public AnalyticsGrpcClient(@Value("${grpc.analytics.host}") String host,
                               @Value("${grpc.analytics.port}") int port) {
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()  // Use TLS in production!
            .build();

        this.blockingStub = AnalyticsServiceGrpc.newBlockingStub(channel);
        this.asyncStub = AnalyticsServiceGrpc.newStub(channel);
    }

    public CampaignStats getCampaignStats(Long campaignId) {
        CampaignStatsRequest request = CampaignStatsRequest.newBuilder()
            .setCampaignId(campaignId)
            .setIncludeDemographics(true)
            .setIncludeTimeline(true)
            .build();

        return blockingStub.getCampaignStats(request);
    }

    public void streamRealTimeMetrics(List<Long> campaignIds,
                                     Consumer<Metric> onMetric) {

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<Metric> responseObserver = new StreamObserver<Metric>() {
            @Override
            public void onNext(Metric metric) {
                onMetric.accept(metric);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in metrics stream", t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<MetricRequest> requestObserver =
            asyncStub.streamMetrics(responseObserver);

        try {
            // Send campaign IDs
            MetricRequest request = MetricRequest.newBuilder()
                .addAllCampaignIds(campaignIds)
                .build();

            requestObserver.onNext(request);
            requestObserver.onCompleted();

            latch.await();
        } catch (Exception e) {
            requestObserver.onError(e);
        }
    }
}

// Usage in REST controller
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsGrpcClient grpcClient;

    @GetMapping("/campaigns/{id}/stats")
    public ResponseEntity<CampaignStatsDto> getStats(@PathVariable Long id) {
        // Call gRPC service instead of local service
        CampaignStats stats = grpcClient.getCampaignStats(id);

        CampaignStatsDto dto = convertToDto(stats);
        return ResponseEntity.ok(dto);
    }
}
```

#### Interview Talking Points

- **HTTP/2 Benefits:** Multiplexing, header compression, binary protocol
- **Protocol Buffers vs JSON:** Size, speed, schema evolution
- **Streaming:** Unary, server streaming, client streaming, bidirectional
- **Service Discovery:** How do clients find gRPC servers?
- **Load Balancing:** Client-side vs server-side
- **Error Handling:** gRPC status codes vs HTTP status codes

**Example Answer:**
> "I split the monolithic email platform into microservices communicating via gRPC. The analytics service handles heavy queries separately, reducing load on the main API. gRPC's binary protocol is 3-5x faster than JSON REST for internal communication, and bi-directional streaming lets us push real-time metrics to dashboards without polling."

---

### 6. Custom Query Language / Expression Engine

**Status:** ⏳ NOT STARTED (SegmentService exists but uses hardcoded filters)
**Priority:** VERY HIGH (Unique skill)
**Estimated Time:** 5-7 days
**Cost:** FREE

#### Why Query Language?

- **Extremely rare skill** - 99% of developers have never built a parser
- Shows compiler/interpreter knowledge
- Demonstrates security awareness (injection prevention)
- Makes your project truly unique
- Similar to: Shopify Liquid, Stripe's query language, MongoDB query language

#### What to Build

**Current Limitation:**
```java
// Hardcoded filters in code
public List<Contact> getActiveGmailUsers() {
    return contactRepository.findByEmailEndingWithAndStatus("@gmail.com", "SUBSCRIBED");
}

// Users can't define their own segments!
```

**With Query Language:**
```java
// Users define segments with expressions:
"email LIKE '%@gmail.com' AND status = 'SUBSCRIBED' AND total_opens > 5"

// Or JSON-based:
{
  "and": [
    {"field": "email", "operator": "endsWith", "value": "@gmail.com"},
    {"field": "status", "operator": "equals", "value": "SUBSCRIBED"},
    {"field": "total_opens", "operator": ">", "value": 5}
  ]
}

// Or user-friendly:
"Engaged Gmail users who opened more than 5 campaigns in the last month"
```

#### Implementation

**Architecture:**

```
User Input
    ↓
Lexer (Tokenization)
    ↓
Parser (Build AST)
    ↓
Validator (Security)
    ↓
Optimizer
    ↓
SQL Generator
    ↓
Execute Query
```

**Step 1: Define Grammar**

```java
// Supported syntax:
// - Comparisons: =, !=, >, <, >=, <=
// - String ops: LIKE, STARTS_WITH, ENDS_WITH, CONTAINS
// - Logical: AND, OR, NOT
// - Grouping: ( )
// - Arrays: IN (value1, value2, ...)
// - Dates: AFTER, BEFORE, BETWEEN

Examples:
"email LIKE '%@gmail.com'"
"status = 'SUBSCRIBED' AND total_opens > 5"
"tags CONTAINS 'premium' AND last_activity AFTER '2024-01-01'"
"(status = 'SUBSCRIBED' OR status = 'PENDING') AND email NOT LIKE '%@example.com'"
"country IN ('US', 'CA', 'UK')"
```

**Step 2: Lexer (Tokenizer)**

```java
public enum TokenType {
    // Literals
    STRING, NUMBER, IDENTIFIER,

    // Operators
    EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN,
    GREATER_EQUALS, LESS_EQUALS,

    // Keywords
    AND, OR, NOT, LIKE, IN, CONTAINS,
    STARTS_WITH, ENDS_WITH, AFTER, BEFORE, BETWEEN,

    // Delimiters
    LEFT_PAREN, RIGHT_PAREN, COMMA,

    // Special
    EOF
}

public class Token {
    private TokenType type;
    private String value;
    private int position;
}

public class Lexer {

    private String input;
    private int position;

    public List<Token> tokenize(String input) {
        this.input = input;
        this.position = 0;

        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            skipWhitespace();

            if (position >= input.length()) break;

            char current = input.charAt(position);

            if (current == '\'') {
                tokens.add(readString());
            } else if (Character.isDigit(current)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(current) || current == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (current == '(') {
                tokens.add(new Token(TokenType.LEFT_PAREN, "(", position));
                position++;
            } else if (current == ')') {
                tokens.add(new Token(TokenType.RIGHT_PAREN, ")", position));
                position++;
            } else if (current == ',') {
                tokens.add(new Token(TokenType.COMMA, ",", position));
                position++;
            } else if (current == '=' && peek() == '=') {
                tokens.add(new Token(TokenType.EQUALS, "==", position));
                position += 2;
            } else if (current == '=') {
                tokens.add(new Token(TokenType.EQUALS, "=", position));
                position++;
            } else if (current == '!' && peek() == '=') {
                tokens.add(new Token(TokenType.NOT_EQUALS, "!=", position));
                position += 2;
            } else if (current == '>') {
                if (peek() == '=') {
                    tokens.add(new Token(TokenType.GREATER_EQUALS, ">=", position));
                    position += 2;
                } else {
                    tokens.add(new Token(TokenType.GREATER_THAN, ">", position));
                    position++;
                }
            } else if (current == '<') {
                if (peek() == '=') {
                    tokens.add(new Token(TokenType.LESS_EQUALS, "<=", position));
                    position += 2;
                } else {
                    tokens.add(new Token(TokenType.LESS_THAN, "<", position));
                    position++;
                }
            } else {
                throw new LexerException("Unexpected character: " + current);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }

    private Token readString() {
        int start = position;
        position++;  // Skip opening quote

        StringBuilder sb = new StringBuilder();
        while (position < input.length() && input.charAt(position) != '\'') {
            if (input.charAt(position) == '\\') {
                position++;  // Skip escape
            }
            sb.append(input.charAt(position));
            position++;
        }

        if (position >= input.length()) {
            throw new LexerException("Unterminated string");
        }

        position++;  // Skip closing quote
        return new Token(TokenType.STRING, sb.toString(), start);
    }

    private Token readNumber() {
        int start = position;
        StringBuilder sb = new StringBuilder();

        while (position < input.length() &&
               (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
            sb.append(input.charAt(position));
            position++;
        }

        return new Token(TokenType.NUMBER, sb.toString(), start);
    }

    private Token readIdentifierOrKeyword() {
        int start = position;
        StringBuilder sb = new StringBuilder();

        while (position < input.length() &&
               (Character.isLetterOrDigit(input.charAt(position)) ||
                input.charAt(position) == '_')) {
            sb.append(input.charAt(position));
            position++;
        }

        String value = sb.toString();
        TokenType type = getKeywordType(value);

        return new Token(type, value, start);
    }

    private TokenType getKeywordType(String value) {
        return switch (value.toUpperCase()) {
            case "AND" -> TokenType.AND;
            case "OR" -> TokenType.OR;
            case "NOT" -> TokenType.NOT;
            case "LIKE" -> TokenType.LIKE;
            case "IN" -> TokenType.IN;
            case "CONTAINS" -> TokenType.CONTAINS;
            case "STARTS_WITH" -> TokenType.STARTS_WITH;
            case "ENDS_WITH" -> TokenType.ENDS_WITH;
            case "AFTER" -> TokenType.AFTER;
            case "BEFORE" -> TokenType.BEFORE;
            case "BETWEEN" -> TokenType.BETWEEN;
            default -> TokenType.IDENTIFIER;
        };
    }
}
```

**Step 3: Parser (Build AST)**

```java
// Abstract Syntax Tree nodes
public interface ExpressionNode {
    <T> T accept(ExpressionVisitor<T> visitor);
}

public class BinaryExpression implements ExpressionNode {
    private ExpressionNode left;
    private TokenType operator;
    private ExpressionNode right;

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitBinary(this);
    }
}

public class ComparisonExpression implements ExpressionNode {
    private String field;
    private TokenType operator;
    private Object value;

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitComparison(this);
    }
}

public class LiteralExpression implements ExpressionNode {
    private Object value;

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLiteral(this);
    }
}

public class Parser {

    private List<Token> tokens;
    private int current = 0;

    public ExpressionNode parse(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
        return expression();
    }

    // expression → or_expression
    private ExpressionNode expression() {
        return orExpression();
    }

    // or_expression → and_expression ( "OR" and_expression )*
    private ExpressionNode orExpression() {
        ExpressionNode expr = andExpression();

        while (match(TokenType.OR)) {
            Token operator = previous();
            ExpressionNode right = andExpression();
            expr = new BinaryExpression(expr, operator.getType(), right);
        }

        return expr;
    }

    // and_expression → comparison ( "AND" comparison )*
    private ExpressionNode andExpression() {
        ExpressionNode expr = comparison();

        while (match(TokenType.AND)) {
            Token operator = previous();
            ExpressionNode right = comparison();
            expr = new BinaryExpression(expr, operator.getType(), right);
        }

        return expr;
    }

    // comparison → field operator value
    private ExpressionNode comparison() {
        if (match(TokenType.LEFT_PAREN)) {
            ExpressionNode expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression");
            return expr;
        }

        if (match(TokenType.NOT)) {
            ExpressionNode expr = comparison();
            return new UnaryExpression(TokenType.NOT, expr);
        }

        Token field = consume(TokenType.IDENTIFIER, "Expected field name");

        if (match(TokenType.EQUALS, TokenType.NOT_EQUALS,
                  TokenType.GREATER_THAN, TokenType.LESS_THAN,
                  TokenType.GREATER_EQUALS, TokenType.LESS_EQUALS)) {
            Token operator = previous();
            ExpressionNode value = primary();
            return new ComparisonExpression(field.getValue(), operator.getType(), value);
        }

        if (match(TokenType.LIKE, TokenType.CONTAINS,
                  TokenType.STARTS_WITH, TokenType.ENDS_WITH)) {
            Token operator = previous();
            Token value = consume(TokenType.STRING, "Expected string value");
            return new ComparisonExpression(field.getValue(), operator.getType(),
                                          value.getValue());
        }

        if (match(TokenType.IN)) {
            consume(TokenType.LEFT_PAREN, "Expected '(' after IN");
            List<Object> values = new ArrayList<>();

            do {
                values.add(primary());
            } while (match(TokenType.COMMA));

            consume(TokenType.RIGHT_PAREN, "Expected ')' after values");
            return new InExpression(field.getValue(), values);
        }

        throw new ParserException("Expected operator after field: " + field.getValue());
    }

    private ExpressionNode primary() {
        if (match(TokenType.STRING)) {
            return new LiteralExpression(previous().getValue());
        }

        if (match(TokenType.NUMBER)) {
            String value = previous().getValue();
            if (value.contains(".")) {
                return new LiteralExpression(Double.parseDouble(value));
            } else {
                return new LiteralExpression(Long.parseLong(value));
            }
        }

        throw new ParserException("Expected value");
    }
}
```

**Step 4: Validator (Security)**

```java
@Service
public class ExpressionValidator {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
        "email", "name", "status", "tags",
        "total_opens", "total_clicks", "last_activity",
        "created_at", "country", "custom_fields"
    );

    public void validate(ExpressionNode ast) {
        ast.accept(new ValidationVisitor());
    }

    private class ValidationVisitor implements ExpressionVisitor<Void> {

        @Override
        public Void visitComparison(ComparisonExpression expr) {
            String field = expr.getField();

            // Prevent SQL injection - only allow whitelisted fields
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new ValidationException("Unknown field: " + field);
            }

            // Validate value type matches field
            Object value = expr.getValue();
            validateValueType(field, value);

            return null;
        }

        @Override
        public Void visitBinary(BinaryExpression expr) {
            expr.getLeft().accept(this);
            expr.getRight().accept(this);
            return null;
        }

        private void validateValueType(String field, Object value) {
            switch (field) {
                case "total_opens", "total_clicks":
                    if (!(value instanceof Number)) {
                        throw new ValidationException(field + " must be a number");
                    }
                    break;

                case "status":
                    if (!List.of("SUBSCRIBED", "UNSUBSCRIBED", "PENDING", "BOUNCED")
                            .contains(value)) {
                        throw new ValidationException("Invalid status: " + value);
                    }
                    break;

                case "last_activity", "created_at":
                    // Validate date format
                    try {
                        LocalDate.parse(value.toString());
                    } catch (Exception e) {
                        throw new ValidationException("Invalid date format: " + value);
                    }
                    break;
            }
        }
    }
}
```

**Step 5: SQL Generator**

```java
@Service
public class SqlGenerator {

    public String generateSql(ExpressionNode ast) {
        return ast.accept(new SqlVisitor());
    }

    private class SqlVisitor implements ExpressionVisitor<String> {

        @Override
        public String visitComparison(ComparisonExpression expr) {
            String field = mapFieldToColumn(expr.getField());
            Object value = expr.getValue();

            return switch (expr.getOperator()) {
                case EQUALS -> field + " = " + formatValue(value);
                case NOT_EQUALS -> field + " != " + formatValue(value);
                case GREATER_THAN -> field + " > " + formatValue(value);
                case LESS_THAN -> field + " < " + formatValue(value);
                case GREATER_EQUALS -> field + " >= " + formatValue(value);
                case LESS_EQUALS -> field + " <= " + formatValue(value);
                case LIKE -> field + " LIKE " + formatValue(value);
                case CONTAINS -> field + " LIKE '%" + escape(value.toString()) + "%'";
                case STARTS_WITH -> field + " LIKE '" + escape(value.toString()) + "%'";
                case ENDS_WITH -> field + " LIKE '%" + escape(value.toString()) + "'";
                default -> throw new IllegalArgumentException("Unknown operator");
            };
        }

        @Override
        public String visitBinary(BinaryExpression expr) {
            String left = expr.getLeft().accept(this);
            String right = expr.getRight().accept(this);

            String operator = switch (expr.getOperator()) {
                case AND -> "AND";
                case OR -> "OR";
                default -> throw new IllegalArgumentException("Unknown operator");
            };

            return "(" + left + " " + operator + " " + right + ")";
        }

        @Override
        public String visitIn(InExpression expr) {
            String field = mapFieldToColumn(expr.getField());
            String values = expr.getValues().stream()
                .map(this::formatValue)
                .collect(Collectors.joining(", "));

            return field + " IN (" + values + ")";
        }

        private String mapFieldToColumn(String field) {
            // Map user-friendly field names to database columns
            return switch (field) {
                case "total_opens" -> "c.open_count";
                case "total_clicks" -> "c.click_count";
                case "last_activity" -> "c.last_activity_at";
                default -> "c." + field;
            };
        }

        private String formatValue(Object value) {
            if (value instanceof String) {
                return "'" + escape(value.toString()) + "'";
            } else if (value instanceof Number) {
                return value.toString();
            } else {
                throw new IllegalArgumentException("Unsupported value type");
            }
        }

        private String escape(String value) {
            // Prevent SQL injection
            return value.replace("'", "''")
                       .replace("\\", "\\\\");
        }
    }
}
```

**Step 6: Query Engine**

```java
@Service
public class SegmentQueryEngine {

    private final Lexer lexer;
    private final Parser parser;
    private final ExpressionValidator validator;
    private final SqlGenerator sqlGenerator;
    private final JdbcTemplate jdbcTemplate;

    public List<Contact> executeQuery(String expression) {
        // 1. Tokenize
        List<Token> tokens = lexer.tokenize(expression);

        // 2. Parse into AST
        ExpressionNode ast = parser.parse(tokens);

        // 3. Validate (security check)
        validator.validate(ast);

        // 4. Optimize (future: query optimization)
        ast = optimize(ast);

        // 5. Generate SQL
        String whereClause = sqlGenerator.generateSql(ast);
        String sql = "SELECT * FROM contacts c WHERE " + whereClause;

        log.info("Generated SQL: {}", sql);

        // 6. Execute
        return jdbcTemplate.query(sql, new ContactRowMapper());
    }

    private ExpressionNode optimize(ExpressionNode ast) {
        // Future: Implement query optimizations
        // - Constant folding
        // - Dead code elimination
        // - Index hint injection
        return ast;
    }
}

// Usage in Segment Service
@Service
public class SegmentService {

    private final SegmentQueryEngine queryEngine;

    public List<Contact> evaluateSegment(Segment segment) {
        String expression = segment.getFilterExpression();
        return queryEngine.executeQuery(expression);
    }
}
```

**Step 7: REST API**

```java
@RestController
@RequestMapping("/api/v1/segments")
public class SegmentController {

    @PostMapping("/test")
    public ResponseEntity<SegmentTestResult> testQuery(
            @RequestBody SegmentTestRequest request) {

        try {
            List<Contact> matches = queryEngine.executeQuery(request.getExpression());

            return ResponseEntity.ok(SegmentTestResult.builder()
                .valid(true)
                .matchCount(matches.size())
                .samples(matches.stream().limit(10).collect(Collectors.toList()))
                .build());

        } catch (LexerException | ParserException | ValidationException e) {
            return ResponseEntity.ok(SegmentTestResult.builder()
                .valid(false)
                .error(e.getMessage())
                .build());
        }
    }

    @PostMapping
    public ResponseEntity<Segment> createSegment(@RequestBody SegmentRequest request) {
        // Validate expression before saving
        try {
            queryEngine.executeQuery(request.getFilterExpression());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Invalid filter expression: " + e.getMessage());
        }

        Segment segment = segmentService.createSegment(request);
        return ResponseEntity.ok(segment);
    }
}
```

#### Interview Talking Points

- **Compiler Theory:** Lexical analysis, syntax analysis, semantic analysis
- **AST (Abstract Syntax Tree):** Structure and traversal
- **Visitor Pattern:** Why it's perfect for AST operations
- **Security:** SQL injection prevention through whitelisting
- **Grammar Design:** Context-free grammars, operator precedence
- **Optimization:** Constant folding, index hints

**Example Answer:**
> "I built a custom query language for user-defined segments using recursive descent parsing. Users can write expressions like 'email LIKE %@gmail.com AND total_opens > 5' which get tokenized, parsed into an AST, validated for security (preventing SQL injection through field whitelisting), and compiled to optimized SQL. This demonstrates compiler design principles - lexer, parser, semantic analysis, and code generation."

This is EXTREMELY impressive in interviews. Very few developers have done this.

---

### 7. Database Sharding Strategy

**Status:** ⏳ NOT STARTED
**Priority:** HIGH
**Estimated Time:** 3-4 days
**Cost:** FREE (just PostgreSQL)

#### Why Sharding?

- Shows horizontal scalability knowledge
- Demonstrates distributed data expertise
- No infrastructure cost (multiple schemas/tables in same DB)
- Critical for high-growth companies

#### What to Implement

**Problem:**
```
Single contacts table with 10M rows → queries slow down
Need to handle 100M+ contacts → single table won't scale
```

**Solution:**
```
Shard contacts across multiple tables/databases
- contacts_shard_0 (20M rows)
- contacts_shard_1 (20M rows)
- contacts_shard_2 (20M rows)
- contacts_shard_3 (20M rows)
- contacts_shard_4 (20M rows)
```

#### Implementation

**Step 1: Sharding Strategy**

```java
public interface ShardingStrategy {
    int getShardId(String shardKey);
    int getTotalShards();
}

@Service
public class HashShardingStrategy implements ShardingStrategy {

    private final int totalShards;

    public HashShardingStrategy(@Value("${sharding.total-shards}") int totalShards) {
        this.totalShards = totalShards;
    }

    @Override
    public int getShardId(String email) {
        // Use consistent hashing
        int hash = email.hashCode();
        return Math.abs(hash % totalShards);
    }

    @Override
    public int getTotalShards() {
        return totalShards;
    }
}

// Alternative: Range-based sharding
@Service
public class RangeShardingStrategy implements ShardingStrategy {

    @Override
    public int getShardId(String email) {
        char firstChar = email.toLowerCase().charAt(0);

        // a-e → shard 0, f-j → shard 1, etc.
        if (firstChar >= 'a' && firstChar <= 'e') return 0;
        if (firstChar >= 'f' && firstChar <= 'j') return 1;
        if (firstChar >= 'k' && firstChar <= 'o') return 2;
        if (firstChar >= 'p' && firstChar <= 't') return 3;
        return 4;  // u-z and numbers
    }

    @Override
    public int getTotalShards() {
        return 5;
    }
}
```

**Step 2: Sharded Repository**

```java
@Repository
public class ShardedContactRepository {

    private final ShardingStrategy shardingStrategy;
    private final Map<Integer, JdbcTemplate> shardTemplates;

    public ShardedContactRepository(
            ShardingStrategy shardingStrategy,
            DataSource dataSource) {

        this.shardingStrategy = shardingStrategy;
        this.shardTemplates = new HashMap<>();

        // Initialize JDBC templates for each shard
        for (int i = 0; i < shardingStrategy.getTotalShards(); i++) {
            shardTemplates.put(i, new JdbcTemplate(dataSource));
        }
    }

    public Contact findByEmail(String email) {
        int shardId = shardingStrategy.getShardId(email);
        String table = "contacts_shard_" + shardId;

        String sql = "SELECT * FROM " + table + " WHERE email = ?";

        try {
            return shardTemplates.get(shardId).queryForObject(
                sql,
                new ContactRowMapper(),
                email
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Contact save(Contact contact) {
        int shardId = shardingStrategy.getShardId(contact.getEmail());
        String table = "contacts_shard_" + shardId;

        if (contact.getId() == null) {
            // Insert
            String sql = """
                INSERT INTO %s (email, name, status, tags, created_at)
                VALUES (?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """.formatted(table);

            Long id = shardTemplates.get(shardId).queryForObject(
                sql,
                Long.class,
                contact.getEmail(),
                contact.getName(),
                contact.getStatus(),
                objectMapper.writeValueAsString(contact.getTags()),
                contact.getCreatedAt()
            );

            contact.setId(id);
        } else {
            // Update
            String sql = """
                UPDATE %s
                SET name = ?, status = ?, tags = ?::jsonb
                WHERE id = ?
                """.formatted(table);

            shardTemplates.get(shardId).update(
                sql,
                contact.getName(),
                contact.getStatus(),
                objectMapper.writeValueAsString(contact.getTags()),
                contact.getId()
            );
        }

        return contact;
    }

    public void delete(String email) {
        int shardId = shardingStrategy.getShardId(email);
        String table = "contacts_shard_" + shardId;

        String sql = "DELETE FROM " + table + " WHERE email = ?";
        shardTemplates.get(shardId).update(sql, email);
    }

    // Cross-shard queries (expensive!)
    public List<Contact> findByStatus(String status) {
        List<Contact> results = new ArrayList<>();

        // Query all shards in parallel
        List<CompletableFuture<List<Contact>>> futures = new ArrayList<>();

        for (int shardId = 0; shardId < shardingStrategy.getTotalShards(); shardId++) {
            final int shard = shardId;

            CompletableFuture<List<Contact>> future = CompletableFuture.supplyAsync(() -> {
                String table = "contacts_shard_" + shard;
                String sql = "SELECT * FROM " + table + " WHERE status = ?";

                return shardTemplates.get(shard).query(
                    sql,
                    new ContactRowMapper(),
                    status
                );
            });

            futures.add(future);
        }

        // Wait for all shards
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Combine results
        for (CompletableFuture<List<Contact>> future : futures) {
            results.addAll(future.join());
        }

        return results;
    }

    public long countAll() {
        // Aggregate counts from all shards
        return IntStream.range(0, shardingStrategy.getTotalShards())
            .parallel()
            .mapToLong(shardId -> {
                String table = "contacts_shard_" + shardId;
                String sql = "SELECT COUNT(*) FROM " + table;
                return shardTemplates.get(shardId).queryForObject(sql, Long.class);
            })
            .sum();
    }
}
```

**Step 3: Shard-Aware Service**

```java
@Service
public class ContactService {

    private final ShardedContactRepository shardedRepository;

    public Contact getContact(String email) {
        // Automatically routes to correct shard
        return shardedRepository.findByEmail(email);
    }

    public Contact createContact(ContactRequest request) {
        Contact contact = Contact.builder()
            .email(request.getEmail())
            .name(request.getName())
            .status(ContactStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();

        return shardedRepository.save(contact);
    }

    public List<Contact> searchContacts(String status, int limit) {
        // Warning: This queries ALL shards - expensive!
        List<Contact> all = shardedRepository.findByStatus(status);

        return all.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

**Step 4: Database Setup**

```sql
-- Create shard tables
CREATE TABLE contacts_shard_0 (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    status VARCHAR(50),
    tags JSONB,
    created_at TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status)
);

CREATE TABLE contacts_shard_1 ( ... same structure ... );
CREATE TABLE contacts_shard_2 ( ... same structure ... );
CREATE TABLE contacts_shard_3 ( ... same structure ... );
CREATE TABLE contacts_shard_4 ( ... same structure ... );

-- Or use schemas for better organization
CREATE SCHEMA shard_0;
CREATE SCHEMA shard_1;
CREATE SCHEMA shard_2;
CREATE SCHEMA shard_3;
CREATE SCHEMA shard_4;

CREATE TABLE shard_0.contacts ( ... );
CREATE TABLE shard_1.contacts ( ... );
-- etc.
```

**Step 5: Rebalancing Tool**

```java
@Service
public class ShardRebalancingService {

    // When adding new shards, rebalance data
    public void rebalance(int oldShardCount, int newShardCount) {
        log.info("Rebalancing from {} to {} shards", oldShardCount, newShardCount);

        for (int oldShard = 0; oldShard < oldShardCount; oldShard++) {
            String table = "contacts_shard_" + oldShard;

            // Read all contacts from old shard
            List<Contact> contacts = jdbcTemplate.query(
                "SELECT * FROM " + table,
                new ContactRowMapper()
            );

            // Redistribute to new shards
            for (Contact contact : contacts) {
                int newShardId = shardingStrategy.getShardId(contact.getEmail());

                if (newShardId != oldShard) {
                    // Move to new shard
                    moveContact(contact, oldShard, newShardId);
                }
            }
        }

        log.info("Rebalancing complete");
    }

    private void moveContact(Contact contact, int fromShard, int toShard) {
        String fromTable = "contacts_shard_" + fromShard;
        String toTable = "contacts_shard_" + toShard;

        // Insert into new shard
        String insertSql = "INSERT INTO " + toTable + " (email, name, status, tags) " +
                          "VALUES (?, ?, ?, ?::jsonb)";
        jdbcTemplate.update(insertSql,
            contact.getEmail(),
            contact.getName(),
            contact.getStatus(),
            objectMapper.writeValueAsString(contact.getTags())
        );

        // Delete from old shard
        String deleteSql = "DELETE FROM " + fromTable + " WHERE id = ?";
        jdbcTemplate.update(deleteSql, contact.getId());
    }
}
```

#### Interview Talking Points

- **Sharding Key Selection:** Email vs ID, immutable keys
- **Cross-Shard Queries:** Performance challenges, scatter-gather
- **Rebalancing:** Consistent hashing, virtual nodes
- **Distributed Transactions:** Two-phase commit, eventual consistency
- **Hotspots:** Handling uneven distribution
- **Foreign Keys:** Challenges with sharded data

**Example Answer:**
> "I implemented horizontal sharding for the contacts table using email hash as the sharding key. With 5 shards, queries for a specific contact go to only one shard, providing 5x throughput. Cross-shard queries use parallel execution across all shards. The main challenge is handling cross-shard aggregations - I cache those results to avoid repeated scatter-gather queries."

---

## 💰 Expensive Technologies (Add Later)

### 8. Apache Kafka - Event Streaming

**Status:** ⏳ NOT STARTED
**Priority:** VERY HIGH (if you can afford it)
**Estimated Time:** 3-4 days
**Cost:** $20-50/month (Confluent Cloud free tier limited)

#### Why Kafka?

- **Highest salary premium** - Kafka expertise is highly valued
- Used by every major tech company
- Perfect for email marketing events
- Real-time analytics foundation

#### Implementation Overview

```java
// Event types
public record EmailSentEvent(Long campaignId, String email, Instant timestamp) {}
public record EmailOpenedEvent(Long campaignId, String email, String location) {}
public record EmailClickedEvent(Long campaignId, String email, String url) {}

// Producer
@Service
public class EmailEventProducer {

    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    public void publishEmailSent(Long campaignId, String email) {
        EmailSentEvent event = new EmailSentEvent(campaignId, email, Instant.now());
        kafkaTemplate.send("email.sent", email, event);
    }
}

// Consumer
@Service
public class AnalyticsConsumer {

    @KafkaListener(topics = "email.opened", groupId = "analytics")
    public void processEmailOpened(EmailOpenedEvent event) {
        // Update real-time analytics
        analyticsService.incrementOpenCount(event.campaignId());
    }
}
```

**When to Add:** After completing Tier 1 technologies, when you have budget for Confluent Cloud or can run Kafka locally.

---

## 📋 Implementation Roadmap

### Phase 1: Quick Wins (Week 1)
- ✅ Redis Advanced Patterns (2-3 days)
- ✅ Prometheus + Grafana (1 day)

**Total:** 3-4 days
**Value:** High observability + performance

---

### Phase 2: Search & Analytics (Week 2)
- ✅ Elasticsearch Implementation (3-4 days)
- ✅ Full-text search for contacts
- ✅ Campaign analytics queries

**Total:** 3-4 days
**Value:** Scalable search + analytics

---

### Phase 3: Reliability (Week 3)
- ✅ Webhook Delivery System (2-3 days)
- ✅ Circuit Breakers for email providers (1 day)

**Total:** 3-4 days
**Value:** Production reliability patterns

---

### Phase 4: Advanced (Week 4+)
- ✅ Custom Query Language (5-7 days) OR
- ✅ gRPC Microservices (4-5 days) OR
- ✅ Database Sharding (3-4 days)

**Pick one based on interest**

---

## 🎯 Recommended Combos

### Combo A: Search & Performance
**Best for:** Backend/Infrastructure roles
1. Elasticsearch
2. Redis Advanced
3. Prometheus/Grafana
4. Database Sharding

**Why:** Shows scalability + performance + observability

---

### Combo B: Architecture & Design
**Best for:** Senior/Staff roles, System Design-heavy interviews
1. Custom Query Language
2. gRPC Microservices
3. Webhook System
4. Event Sourcing (if time)

**Why:** Shows architectural thinking + unique skills

---

### Combo C: Full Stack Excellence
**Best for:** Full-stack or Product Engineer roles
1. Elasticsearch (search features)
2. WebSocket Real-time (user experience)
3. Webhook System (integrations)
4. Prometheus (metrics)

**Why:** Complete product with modern UX

---

## 💼 Interview Value

### When Asked "Tell me about a complex project..."

**With these technologies, you can say:**

> "I built OpenMailer, an email marketing platform handling 100k+ emails daily. I implemented **Elasticsearch** for full-text search across 10M contacts, achieving sub-100ms query times vs 10+ seconds in PostgreSQL.
>
> For reliability, I built a **webhook delivery system** with exponential backoff and dead letter queues, similar to Stripe's architecture.
>
> I added **advanced Redis patterns** including distributed locking to prevent duplicate campaign sends across multiple servers, and Lua-scripted rate limiting for precise API throttling.
>
> The most challenging part was building a **custom query language** for user-defined segments. I implemented a full lexer/parser that compiles user expressions into optimized SQL while preventing injection attacks through AST validation.
>
> I instrumented everything with **Prometheus metrics** and built **Grafana dashboards** showing P95 latency, error rates, and business metrics in real-time."

**This demonstrates:**
- ✅ Scalability (Elasticsearch, Redis, Sharding)
- ✅ Reliability (Webhooks, Circuit Breakers)
- ✅ Performance (Query optimization, caching)
- ✅ Observability (Prometheus, Grafana, Tracing)
- ✅ Security (Injection prevention)
- ✅ Unique Skills (Query language parser)
- ✅ Production Thinking (Metrics, monitoring, fault tolerance)

**Companies that value this:**
- Stripe, Twilio, SendGrid (webhooks, reliability)
- Shopify, Amazon (scalability, search)
- Datadog, New Relic (observability)
- Any fintech, SaaS, or high-growth startup

---

## 🚀 Getting Started

**Start here (1 week total):**

1. **Redis Advanced Patterns** (Day 1-3)
   - Distributed locks
   - Rate limiting with Lua
   - Pub/Sub

2. **Prometheus + Grafana** (Day 4)
   - Add metrics
   - Create dashboard

3. **Elasticsearch** (Day 5-7)
   - Contact search
   - Basic analytics

**After completing the above, you'll have:**
- ✅ Highly marketable skills
- ✅ Production-ready features
- ✅ Strong interview talking points
- ✅ $0 infrastructure cost

---

## 📚 Resources

### Elasticsearch
- Official Docs: https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
- Spring Data Elasticsearch: https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/

### Redis
- Redis University (Free): https://university.redis.com/
- Patterns: https://redis.io/docs/manual/patterns/

### Prometheus/Grafana
- Prometheus Docs: https://prometheus.io/docs/introduction/overview/
- Grafana Dashboards: https://grafana.com/grafana/dashboards/

### gRPC
- gRPC Docs: https://grpc.io/docs/languages/java/
- Protocol Buffers: https://protobuf.dev/

### Compiler Design (Query Language)
- Crafting Interpreters (Free book): https://craftinginterpreters.com/
- Coursera Compilers Course: https://www.coursera.org/learn/compilers

---

## 📊 Cost Summary

| Technology | Monthly Cost | Notes |
|-----------|--------------|-------|
| Elasticsearch | $0 | Docker local |
| Redis | $0 | Docker local |
| Prometheus/Grafana | $0 | Docker local |
| PostgreSQL (Sharding) | $0 | Same DB |
| gRPC | $0 | No infrastructure |
| Webhook System | $0 | Built-in |
| Query Language | $0 | Pure code |
| **Kafka** | $20-50 | Only if using Confluent Cloud |

**Total for Tier 1 + Tier 2:** $0/month

---

---

## 🌟 Tier 3: Unique & Specialized (Stand Out From the Crowd)

These are **unconventional** technologies that 95%+ of developers never touch. Implementing even ONE of these makes you memorable in interviews.

---

### 9. Machine Learning - Send Time Optimization & Spam Scoring

**Status:** ⏳ NOT STARTED
**Priority:** VERY HIGH (Unique)
**Estimated Time:** 5-7 days
**Cost:** FREE (scikit-learn via Python, or Deeplearning4j for Java)

#### Why ML is Unique?

- **Extremely rare** - Very few backend devs have production ML experience
- Shows data science crossover skills
- Solves real business problems (higher open rates = revenue)
- Makes your project feel like a real product, not just CRUD

#### What to Build

**A. Send Time Optimization**

*Problem:* When should you send an email to maximize open rate?
- User A opens emails at 9 AM on weekdays
- User B opens emails at 8 PM on weekends
- User C never opens emails (dead lead)

*ML Solution:* Learn each user's behavior and predict optimal send time.

```python
# Python service (call from Java via HTTP or gRPC)
from sklearn.ensemble import RandomForestClassifier
import pandas as pd
import numpy as np
from datetime import datetime

class SendTimeOptimizer:

    def __init__(self):
        self.model = RandomForestClassifier(n_estimators=100)
        self.trained = False

    def train(self, email_events_df):
        """
        Train on historical email open data

        Features:
        - hour_of_day (0-23)
        - day_of_week (0-6)
        - contact_timezone
        - historical_open_rate
        - device_type (mobile/desktop)

        Label: opened (1) or not (0)
        """
        features = self._extract_features(email_events_df)
        labels = email_events_df['opened']

        self.model.fit(features, labels)
        self.trained = True

    def predict_best_send_time(self, contact_id):
        """
        For a given contact, test all 24 hours and return best time
        """
        if not self.trained:
            raise Exception("Model not trained")

        contact_data = self._get_contact_history(contact_id)

        # Test all hours
        scores = []
        for hour in range(24):
            features = self._build_features(contact_data, hour)
            probability = self.model.predict_proba([features])[0][1]
            scores.append((hour, probability))

        # Return hour with highest open probability
        best_hour, probability = max(scores, key=lambda x: x[1])

        return {
            "optimal_hour": best_hour,
            "predicted_open_rate": probability,
            "confidence": self._calculate_confidence(contact_data)
        }

    def _extract_features(self, df):
        """Extract ML features from email events"""
        return df[['hour_sent', 'day_of_week', 'timezone_offset',
                   'historical_opens', 'is_mobile']]

    def _calculate_confidence(self, contact_data):
        """How confident are we? Based on amount of historical data"""
        num_emails = len(contact_data)
        if num_emails < 5:
            return "LOW"
        elif num_emails < 20:
            return "MEDIUM"
        else:
            return "HIGH"
```

**Java Integration:**

```java
@Service
public class SendTimeOptimizationService {

    private final RestTemplate restTemplate;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    public OptimalSendTime predictBestSendTime(Long contactId) {
        String url = mlServiceUrl + "/predict?contact_id=" + contactId;

        OptimalSendTime result = restTemplate.getForObject(
            url,
            OptimalSendTime.class
        );

        return result;
    }

    public void scheduleCampaignWithOptimization(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow();

        List<Contact> recipients = campaign.getRecipients();

        // Group by optimal send time
        Map<Integer, List<Contact>> groupedByHour = new HashMap<>();

        for (Contact contact : recipients) {
            OptimalSendTime optimal = predictBestSendTime(contact.getId());
            int hour = optimal.getOptimalHour();

            groupedByHour
                .computeIfAbsent(hour, k -> new ArrayList<>())
                .add(contact);
        }

        // Create sub-campaigns for each hour
        for (Map.Entry<Integer, List<Contact>> entry : groupedByHour.entrySet()) {
            int hour = entry.getKey();
            List<Contact> contacts = entry.getValue();

            LocalDateTime sendTime = LocalDate.now()
                .atTime(hour, 0);

            scheduleBatchSend(campaign, contacts, sendTime);
        }
    }
}
```

**B. Spam Score Prediction**

*Problem:* Will this email land in spam folder?

```python
class SpamScorePredictor:

    def __init__(self):
        # Train on historical emails that went to spam vs inbox
        self.model = self._load_pretrained_model()

    def predict_spam_score(self, email_content):
        """
        Returns spam probability (0-1)

        Features:
        - Word count, character count
        - Spam trigger words frequency
        - HTML/text ratio
        - Image count
        - Link count
        - Capitalization ratio
        - Exclamation mark count
        - Has unsubscribe link
        - Sender reputation
        """
        features = self._extract_email_features(email_content)

        spam_probability = self.model.predict_proba([features])[0][1]

        return {
            "spam_score": spam_probability,
            "risk_level": self._get_risk_level(spam_probability),
            "suggestions": self._generate_suggestions(features, spam_probability)
        }

    def _generate_suggestions(self, features, spam_prob):
        """Give actionable advice to reduce spam score"""
        suggestions = []

        if features['capitalization_ratio'] > 0.3:
            suggestions.append("Reduce ALL CAPS text - high capitalization detected")

        if features['exclamation_count'] > 3:
            suggestions.append("Remove excessive exclamation marks")

        if features['spam_words_count'] > 5:
            suggestions.append("Reduce spam trigger words like 'FREE', 'BUY NOW', 'LIMITED TIME'")

        if not features['has_unsubscribe']:
            suggestions.append("Add unsubscribe link (legal requirement)")

        if features['link_density'] > 0.2:
            suggestions.append("Too many links - reduce link count")

        return suggestions
```

**C. Subject Line Scoring**

*Problem:* Which subject line will get more opens?

```python
class SubjectLineOptimizer:

    def __init__(self):
        # Trained on millions of subject lines
        self.model = self._load_model()

    def score_subject_line(self, subject):
        """
        Predicts open rate for subject line
        """
        features = self._extract_subject_features(subject)

        predicted_open_rate = self.model.predict([features])[0]

        return {
            "predicted_open_rate": predicted_open_rate,
            "grade": self._get_grade(predicted_open_rate),
            "suggestions": self._generate_suggestions(subject, features)
        }

    def _extract_subject_features(self, subject):
        """Extract features from subject line"""
        return {
            "length": len(subject),
            "word_count": len(subject.split()),
            "has_emoji": self._contains_emoji(subject),
            "has_personalization": "{{" in subject,
            "starts_with_number": subject[0].isdigit() if subject else False,
            "has_question": "?" in subject,
            "sentiment": self._get_sentiment(subject),
            "urgency_words": self._count_urgency_words(subject),
            "curiosity_gap": self._detect_curiosity_gap(subject)
        }

    def _generate_suggestions(self, subject, features):
        suggestions = []

        if features['length'] > 60:
            suggestions.append("Subject too long - keep under 60 characters")

        if features['length'] < 20:
            suggestions.append("Subject too short - aim for 30-50 characters")

        if not features['has_personalization']:
            suggestions.append("Add personalization like {{name}} to increase opens")

        if features['word_count'] > 10:
            suggestions.append("Too many words - be more concise")

        return suggestions

    def compare_subject_lines(self, subjects):
        """A/B testing multiple subject lines"""
        scores = []

        for subject in subjects:
            score = self.score_subject_line(subject)
            scores.append({
                "subject": subject,
                "score": score
            })

        # Rank by predicted open rate
        scores.sort(key=lambda x: x['score']['predicted_open_rate'], reverse=True)

        return scores
```

**Java Controller:**

```java
@RestController
@RequestMapping("/api/v1/ml")
public class MachineLearningController {

    @PostMapping("/spam-score")
    public ResponseEntity<SpamScoreResult> checkSpam(
            @RequestBody EmailContent email) {

        SpamScoreResult result = mlService.predictSpamScore(email);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/optimize-subject")
    public ResponseEntity<SubjectScoreResult> scoreSubject(
            @RequestBody SubjectLineRequest request) {

        SubjectScoreResult result = mlService.scoreSubjectLine(
            request.getSubject()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/compare-subjects")
    public ResponseEntity<List<SubjectComparison>> compareSubjects(
            @RequestBody List<String> subjects) {

        // Returns ranked list
        List<SubjectComparison> ranked = mlService.compareSubjects(subjects);
        return ResponseEntity.ok(ranked);
    }

    @GetMapping("/contacts/{id}/best-send-time")
    public ResponseEntity<OptimalSendTime> getBestSendTime(
            @PathVariable Long id) {

        OptimalSendTime result = mlService.predictBestSendTime(id);
        return ResponseEntity.ok(result);
    }
}
```

#### Interview Talking Points

**When asked about ML:**
> "I implemented ML-powered send time optimization using Random Forest. The model learns from historical open events - features like hour of day, day of week, and user timezone. For a user who typically opens emails at 9 AM on weekdays, the model predicts 9 AM sends will have 65% open rate vs 15% at 3 PM. This increased average campaign open rates by 23%."

**Technical depth:**
- Feature engineering decisions
- Train/test split methodology
- Handling class imbalance (most emails not opened)
- Model retraining strategy
- A/B testing ML predictions vs random sends

#### Dependencies

```xml
<!-- Option 1: Call Python service via HTTP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Option 2: Use Java ML library -->
<dependency>
    <groupId>org.deeplearning4j</groupId>
    <artifactId>deeplearning4j-core</artifactId>
    <version>1.0.0-M2.1</version>
</dependency>

<!-- Option 3: Call Python via gRPC -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.60.0</version>
</dependency>
```

---

### 10. CDC (Change Data Capture) with Debezium

**Status:** ⏳ NOT STARTED
**Priority:** HIGH (Unique)
**Estimated Time:** 3-4 days
**Cost:** FREE

#### Why CDC is Unique?

- **Extremely specialized** - 99% of developers don't know CDC
- Shows event-driven architecture expertise
- Kafka-like benefits without Kafka complexity
- Used by: Uber, Netflix, Airbnb

#### What is CDC?

Instead of:
```java
// Application code manually publishes events
public void saveContact(Contact contact) {
    contactRepository.save(contact);
    eventPublisher.publish(new ContactCreatedEvent(contact)); // Manual!
}
```

With CDC:
```
PostgreSQL → Debezium → Event Stream
(automatic database change tracking)
```

**Every database change becomes an event automatically!**

#### Implementation

**Architecture:**
```
PostgreSQL (with replication)
    ↓
Debezium Connector (reads WAL - Write-Ahead Log)
    ↓
Event Stream (contacts.created, contacts.updated, contacts.deleted)
    ↓
Multiple Consumers:
    - Search Index Updater → Update Elasticsearch
    - Analytics Service → Update metrics
    - Webhook Delivery → Notify external systems
    - Audit Log Service → Compliance tracking
```

**Step 1: Enable PostgreSQL Replication**

```sql
-- postgresql.conf
wal_level = logical
max_replication_slots = 4
max_wal_senders = 4
```

**Step 2: Debezium Configuration**

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: openmailer
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    command:
      - "postgres"
      - "-c"
      - "wal_level=logical"

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092

  debezium:
    image: debezium/connect:latest
    depends_on:
      - kafka
      - postgres
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: debezium_configs
      OFFSET_STORAGE_TOPIC: debezium_offsets
```

**Step 3: Configure Connector**

```json
POST http://localhost:8083/connectors
{
  "name": "openmailer-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "user",
    "database.password": "password",
    "database.dbname": "openmailer",
    "database.server.name": "openmailer",
    "table.include.list": "public.contacts,public.campaigns,public.email_events",
    "plugin.name": "pgoutput"
  }
}
```

**Step 4: Consume Events**

```java
@Service
public class ContactChangeConsumer {

    @KafkaListener(topics = "openmailer.public.contacts", groupId = "search-indexer")
    public void handleContactChange(String event) {
        DebeziumEvent<Contact> changeEvent = parseEvent(event);

        switch (changeEvent.getOperation()) {
            case CREATE, UPDATE -> {
                Contact contact = changeEvent.getAfter();
                // Automatically update Elasticsearch
                elasticsearchService.indexContact(contact);
            }
            case DELETE -> {
                Contact contact = changeEvent.getBefore();
                elasticsearchService.deleteContact(contact.getId());
            }
        }
    }
}

@Service
public class AnalyticsChangeConsumer {

    @KafkaListener(topics = "openmailer.public.email_events", groupId = "analytics")
    public void handleEmailEvent(String event) {
        DebeziumEvent<EmailEvent> changeEvent = parseEvent(event);

        if (changeEvent.getOperation() == Operation.CREATE) {
            EmailEvent emailEvent = changeEvent.getAfter();

            // Real-time analytics update
            if (emailEvent.getType().equals("OPENED")) {
                metricsService.incrementOpenCount(emailEvent.getCampaignId());

                // Push to real-time dashboard
                webSocketService.broadcast(
                    "campaign:" + emailEvent.getCampaignId(),
                    Map.of("type", "open", "timestamp", System.currentTimeMillis())
                );
            }
        }
    }
}
```

**Benefits:**

1. **Automatic Event Publishing** - No manual eventPublisher.publish() calls
2. **Zero Downtime** - Consumers process changes in real-time
3. **Guaranteed Delivery** - Database changes can't be lost
4. **Audit Trail** - Full history of all changes
5. **Microservices Sync** - Keep read models up to date

#### Interview Talking Points

> "I implemented CDC using Debezium to automatically stream database changes to Elasticsearch. When a contact is created in PostgreSQL, Debezium captures the WAL entry and publishes an event. Multiple consumers update search indexes, analytics, and webhooks - all without a single line of event publishing code in the application. This guarantees consistency between the database and search indexes."

**Technical depth:**
- WAL (Write-Ahead Log) internals
- Event sourcing vs CDC
- Handling schema evolution
- At-least-once delivery guarantees
- Offset management

---

### 11. GraphQL API Layer

**Status:** ⏳ NOT STARTED
**Priority:** MEDIUM (Modern)
**Estimated Time:** 3-4 days
**Cost:** FREE

#### Why GraphQL?

- Modern API standard (Facebook, GitHub, Shopify)
- Shows frontend/API design thinking
- Solves over-fetching problem
- Different from everyone doing REST

#### What to Build

Instead of:
```
GET /api/v1/campaigns/123
GET /api/v1/campaigns/123/stats
GET /api/v1/campaigns/123/recipients
GET /api/v1/templates/456
```

With GraphQL:
```graphql
query {
  campaign(id: 123) {
    name
    subject
    stats {
      openRate
      clickRate
    }
    recipients {
      email
      status
    }
    template {
      content
    }
  }
}
```

**One request, exactly the data you need!**

#### Implementation

```java
// Schema definition
type Campaign {
  id: ID!
  name: String!
  subject: String!
  status: CampaignStatus!
  stats: CampaignStats
  recipients: [Contact!]!
  template: Template
  createdAt: DateTime!
}

type CampaignStats {
  sentCount: Int!
  openedCount: Int!
  clickedCount: Int!
  openRate: Float!
  clickRate: Float!
}

type Query {
  campaign(id: ID!): Campaign
  campaigns(status: CampaignStatus, limit: Int): [Campaign!]!
  contact(email: String!): Contact
  searchContacts(query: String!): [Contact!]!
}

type Mutation {
  createCampaign(input: CreateCampaignInput!): Campaign!
  sendCampaign(id: ID!): Boolean!
  updateContact(input: UpdateContactInput!): Contact!
}

type Subscription {
  campaignProgress(campaignId: ID!): CampaignProgress!
  realTimeStats(campaignId: ID!): CampaignStats!
}
```

```java
@Component
public class CampaignResolver implements GraphQLQueryResolver {

    @Autowired
    private CampaignService campaignService;

    public Campaign campaign(Long id) {
        return campaignService.getCampaign(id);
    }

    public List<Campaign> campaigns(CampaignStatus status, Integer limit) {
        return campaignService.getCampaigns(status, limit);
    }
}

@Component
public class CampaignMutationResolver implements GraphQLMutationResolver {

    public Campaign createCampaign(CreateCampaignInput input) {
        return campaignService.createCampaign(input);
    }

    public Boolean sendCampaign(Long id) {
        campaignService.sendCampaign(id);
        return true;
    }
}

// Real-time subscriptions!
@Component
public class CampaignSubscriptionResolver implements GraphQLSubscriptionResolver {

    public Publisher<CampaignProgress> campaignProgress(Long campaignId) {
        return Flux.create(emitter -> {
            // Subscribe to Redis pub/sub
            messageListener.subscribe("campaign:" + campaignId, message -> {
                emitter.next(parseCampaignProgress(message));
            });
        });
    }
}
```

#### Dependencies

```xml
<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphql-spring-boot-starter</artifactId>
    <version>15.0.0</version>
</dependency>

<dependency>
    <groupId>com.graphql-java-kickstart</groupId>
    <artifactId>graphiql-spring-boot-starter</artifactId>
    <version>15.0.0</version>
</dependency>
```

---

### 12. Workflow Orchestration with Temporal

**Status:** ⏳ NOT STARTED
**Priority:** MEDIUM (Advanced)
**Estimated Time:** 4-5 days
**Cost:** FREE (self-hosted)

#### Why Temporal?

- **Extremely unique** - Almost no one knows Temporal
- Used by: Netflix, Stripe, Datadog
- Solves complex async workflow problems
- Shows distributed systems mastery

#### What is Temporal?

Manage long-running, multi-step workflows with automatic retries and state management.

**Example: Campaign Send Workflow**

```java
@WorkflowInterface
public interface CampaignWorkflow {
    @WorkflowMethod
    void executeCampaign(Long campaignId);
}

@WorkflowImpl
public class CampaignWorkflowImpl implements CampaignWorkflow {

    private final EmailActivities activities =
        Workflow.newActivityStub(EmailActivities.class);

    @Override
    public void executeCampaign(Long campaignId) {
        // Step 1: Validate campaign
        ValidationResult validation = activities.validateCampaign(campaignId);
        if (!validation.isValid()) {
            throw new IllegalStateException("Invalid campaign");
        }

        // Step 2: Warm up IP (if new domain)
        if (validation.needsIpWarmup()) {
            activities.performIpWarmup(campaignId);
            // Wait 2 hours between warmup batches
            Workflow.sleep(Duration.ofHours(2));
        }

        // Step 3: Send to test list first
        activities.sendToTestList(campaignId);

        // Step 4: Wait for human approval
        Workflow.await(() -> activities.isApproved(campaignId));

        // Step 5: Send to main list in batches
        List<Long> batches = activities.createBatches(campaignId);

        for (Long batchId : batches) {
            // Send batch
            activities.sendBatch(batchId);

            // Rate limit: wait between batches
            Workflow.sleep(Duration.ofMinutes(5));

            // Check deliverability - if bounce rate too high, stop
            BatchStats stats = activities.getBatchStats(batchId);
            if (stats.getBounceRate() > 0.10) {
                activities.pauseCampaign(campaignId);
                throw new IllegalStateException("High bounce rate detected");
            }
        }

        // Step 6: Wait 24 hours for opens/clicks
        Workflow.sleep(Duration.ofHours(24));

        // Step 7: Send follow-up to non-openers
        activities.sendFollowUp(campaignId);

        // Step 8: Generate final report
        activities.generateReport(campaignId);
    }
}
```

**Benefits:**
- Automatic retry on failures
- Workflow state persisted (survives server crashes)
- Can pause/resume workflows
- Built-in versioning
- Visual workflow timeline

#### Interview Value

> "I used Temporal for campaign orchestration. A campaign workflow includes validation, IP warmup, test sends, approval gates, batched sending with rate limiting, deliverability checks, and follow-up sequences. If the server crashes during Step 3, Temporal automatically resumes from that point when it restarts. This is much more robust than cron jobs or manual state machines."

---

### 13. Vector Search with pgvector

**Status:** ⏳ NOT STARTED
**Priority:** MEDIUM (AI/ML trend)
**Estimated Time:** 2-3 days
**Cost:** FREE

#### Why Vector Search?

- **Trending** - AI/embeddings are hot
- Semantic search (not just keyword matching)
- Shows ML/AI awareness
- Free extension for PostgreSQL

#### What to Build

**Semantic Template Search:**

Instead of:
```sql
-- Keyword search (exact match)
SELECT * FROM templates WHERE content LIKE '%discount%'
```

With Vector Search:
```sql
-- Semantic search (meaning-based)
SELECT * FROM templates
ORDER BY embedding <-> query_embedding('promotional sale')
LIMIT 10
```

**Finds templates about sales even if they don't contain word "sale"!**

#### Implementation

```java
@Service
public class TemplateSemanticSearch {

    @Autowired
    private OpenAIService openAI;  // Or Hugging Face

    public List<Template> semanticSearch(String query) {
        // 1. Convert query to vector embedding
        float[] queryEmbedding = openAI.getEmbedding(query);

        // 2. Find similar templates using cosine similarity
        String sql = """
            SELECT *,
                   embedding <-> ?::vector AS distance
            FROM templates
            ORDER BY distance
            LIMIT 10
            """;

        return jdbcTemplate.query(sql,
            new TemplateRowMapper(),
            Arrays.toString(queryEmbedding)
        );
    }

    public void indexTemplate(Template template) {
        // Generate embedding for template content
        float[] embedding = openAI.getEmbedding(template.getContent());

        // Store in database
        jdbcTemplate.update(
            "UPDATE templates SET embedding = ?::vector WHERE id = ?",
            Arrays.toString(embedding),
            template.getId()
        );
    }
}
```

**Use Cases:**
- "Find templates similar to this one"
- "Search by campaign goal, not keywords"
- "Recommend templates based on past campaigns"

---

### 14. A/B Testing Framework

**Status:** ⏳ NOT STARTED
**Priority:** HIGH (Product feature)
**Estimated Time:** 3-4 days
**Cost:** FREE

#### Why A/B Testing?

- **Product-minded** - Shows you think about business metrics
- Real feature users want
- Statistical knowledge (not just coding)
- Differentiates from pure backend devs

#### What to Build

```java
@Entity
public class ABTest {
    @Id
    private Long id;

    private Long campaignId;
    private String testName;  // "Subject Line Test"
    private ABTestType type;  // SUBJECT, SENDER_NAME, CONTENT, SEND_TIME

    @OneToMany
    private List<ABVariant> variants;

    private Integer sampleSize;  // 10% of list
    private LocalDateTime startedAt;
    private LocalDateTime winningSentAt;

    private ABTestStatus status;  // RUNNING, COMPLETED
    private Long winningVariantId;
}

@Entity
public class ABVariant {
    @Id
    private Long id;

    private String name;  // "Variant A", "Variant B"
    private String value;  // The subject line, or content

    // Results
    private Integer sentCount;
    private Integer openedCount;
    private Integer clickedCount;
    private Double openRate;
    private Double clickRate;

    // Statistical significance
    private Double confidenceLevel;  // 95%
    private Boolean isWinner;
}

@Service
public class ABTestService {

    public ABTest createSubjectLineTest(Long campaignId,
                                       List<String> subjects,
                                       Integer sampleSize) {

        ABTest test = new ABTest();
        test.setCampaignId(campaignId);
        test.setType(ABTestType.SUBJECT);
        test.setSampleSize(sampleSize);

        // Create variants
        for (int i = 0; i < subjects.size(); i++) {
            ABVariant variant = new ABVariant();
            variant.setName("Variant " + (char)('A' + i));
            variant.setValue(subjects.get(i));
            test.addVariant(variant);
        }

        return abTestRepository.save(test);
    }

    public void executeABTest(ABTest test) {
        Campaign campaign = campaignRepository.findById(test.getCampaignId())
            .orElseThrow();

        List<Contact> recipients = campaign.getRecipients();

        // Take sample (e.g., 10% of list)
        int sampleCount = (recipients.size() * test.getSampleSize()) / 100;
        List<Contact> sample = recipients.subList(0, sampleCount);

        // Split evenly among variants
        int variantSize = sample.size() / test.getVariants().size();

        for (int i = 0; i < test.getVariants().size(); i++) {
            ABVariant variant = test.getVariants().get(i);
            List<Contact> variantRecipients = sample.subList(
                i * variantSize,
                (i + 1) * variantSize
            );

            // Send with this variant
            sendVariant(campaign, variant, variantRecipients);
        }

        test.setStatus(ABTestStatus.RUNNING);
        test.setStartedAt(LocalDateTime.now());
        abTestRepository.save(test);

        // Schedule winner selection (e.g., 4 hours later)
        scheduleWinnerSelection(test.getId(), Duration.ofHours(4));
    }

    @Scheduled(fixedDelay = 300000)  // Every 5 minutes
    public void checkCompletedTests() {
        List<ABTest> runningTests = abTestRepository.findByStatus(ABTestStatus.RUNNING);

        for (ABTest test : runningTests) {
            if (shouldSelectWinner(test)) {
                selectWinner(test);
            }
        }
    }

    private void selectWinner(ABTest test) {
        // Update stats for each variant
        for (ABVariant variant : test.getVariants()) {
            updateVariantStats(variant);
        }

        // Statistical analysis
        ABVariant winner = findStatisticalWinner(test.getVariants());

        if (winner != null) {
            test.setWinningVariantId(winner.getId());
            test.setStatus(ABTestStatus.COMPLETED);
            winner.setIsWinner(true);

            // Send to remaining recipients using winning variant
            sendToRemainingRecipients(test, winner);
        } else {
            // No clear winner - use variant A by default
            log.warn("No statistically significant winner for test {}", test.getId());
        }

        abTestRepository.save(test);
    }

    private ABVariant findStatisticalWinner(List<ABVariant> variants) {
        // Chi-squared test for statistical significance
        double[][] observations = new double[variants.size()][2];

        for (int i = 0; i < variants.size(); i++) {
            ABVariant v = variants.get(i);
            observations[i][0] = v.getOpenedCount();
            observations[i][1] = v.getSentCount() - v.getOpenedCount();
        }

        ChiSquareTest chiSquare = new ChiSquareTest();
        double pValue = chiSquare.chiSquareTest(observations);

        // If p-value < 0.05, there's a significant difference
        if (pValue < 0.05) {
            // Return variant with highest open rate
            return variants.stream()
                .max(Comparator.comparing(ABVariant::getOpenRate))
                .orElse(null);
        }

        return null;  // No significant winner
    }
}
```

#### Dependencies

```xml
<!-- Apache Commons Math for statistical tests -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-math3</artifactId>
    <version>3.6.1</version>
</dependency>
```

#### Interview Talking Points

> "I built an A/B testing framework for campaign optimization. Users can test multiple subject lines by sending each to a 10% sample. After 4 hours, I run a chi-squared test to determine if one variant has a statistically significant higher open rate. If so, the winner is automatically sent to the remaining 90%. This increased average open rates by 18% by letting data choose the best subject line."

**Shows:**
- Statistical knowledge (p-values, confidence intervals)
- Product thinking (feature that drives business value)
- Async workflow management

---

## 🎯 Updated Priority Matrix

| Technology | Uniqueness | Interview Impact | Difficulty | Time |
|-----------|-----------|------------------|-----------|------|
| **Machine Learning** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | HIGH | 5-7 days |
| **CDC (Debezium)** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | MEDIUM | 3-4 days |
| **A/B Testing** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | MEDIUM | 3-4 days |
| **Temporal Workflows** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | HIGH | 4-5 days |
| **Vector Search** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | LOW | 2-3 days |
| **GraphQL** | ⭐⭐⭐ | ⭐⭐⭐⭐ | LOW | 3-4 days |

---

## 💡 Best Combinations

### **The "AI/ML Engineer" Combo:**
1. Machine Learning (Send Time Optimization)
2. Vector Search (Semantic templates)
3. A/B Testing (Statistical analysis)

**Interview pitch:** "I built ML features including send time optimization, semantic search, and statistically rigorous A/B testing."

---

### **The "Data Engineer" Combo:**
1. CDC with Debezium
2. TimescaleDB (time-series analytics)
3. Elasticsearch

**Interview pitch:** "I built a real-time data pipeline using CDC to stream database changes to analytics systems."

---

### **The "Product Engineer" Combo:**
1. A/B Testing Framework
2. GraphQL API
3. Machine Learning (Subject line scoring)

**Interview pitch:** "I built product features that directly improve business metrics - A/B testing increased open rates by 18%."

---

### **The "Distributed Systems Expert" Combo:**
1. Temporal Workflows
2. CDC (Debezium)
3. gRPC Microservices

**Interview pitch:** "I architected a distributed system using workflow orchestration, change data capture, and gRPC for service communication."

---

## 🚀 My Recommendation

**Pick ONE from each tier:**

**Tier 1 (Foundation):** Elasticsearch + Redis Advanced
**Tier 2 (Architecture):** Custom Query Language OR gRPC
**Tier 3 (Unique):** Machine Learning OR A/B Testing

This gives you:
- ✅ Scalability skills (Elasticsearch, Redis)
- ✅ Unique skill (Query Language or ML)
- ✅ Product thinking (A/B Testing)
- ✅ Zero infrastructure cost

**Total time:** ~2-3 weeks
**Interview impact:** 🚀🚀🚀🚀🚀

---

**Questions? Need help implementing any of these? Just ask!**
