# Railway Deployment Guide

## Overview

This guide covers deploying OpenMailer on [Railway](https://railway.app). Railway provides managed PostgreSQL and Redis as plugins, and deploys the Spring Boot app via Dockerfile or Nixpacks. **BunkerWeb cannot run on Railway** (it requires Docker socket access for autoconf mode), so this guide covers the Railway-native security alternative and an optional hybrid approach using BunkerWeb on a separate VPS.

---

## Architecture on Railway

```
Internet
    │
    ▼
Cloudflare (WAF + DDoS + SSL)        ← replaces BunkerWeb on Railway
    │
    ▼
Railway Project
├── openmailer-app   (Spring Boot, built from Dockerfile)
├── postgres         (Railway managed PostgreSQL plugin)
└── redis            (Railway managed Redis plugin)
```

For full BunkerWeb WAF parity, see the [Hybrid Approach](#optional-hybrid-approach-bunkerweb-on-vps) at the bottom.

---

## Prerequisites

- Railway account with a project created
- Railway CLI installed: `npm install -g @railway/cli`
- GitHub repo connected to Railway project
- A domain (e.g. `openmailer.site`) with DNS managed in Cloudflare

---

## Step 1 — Add Managed PostgreSQL

1. In your Railway project → **New Service → Database → PostgreSQL**
2. Railway provisions a PostgreSQL 17 instance automatically
3. Click the Postgres service → **Variables** tab — note these auto-generated values:
   ```
   PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE
   POSTGRES_URL   (full JDBC-style connection string)
   DATABASE_URL   (postgres:// URL)
   ```
4. Railway exposes a private hostname: `postgres.railway.internal:5432`

**Equivalent to** `postgres:` service in `compose.bunkerweb-1.6.9.yml`.

---

## Step 2 — Add Managed Redis

1. In your Railway project → **New Service → Database → Redis**
2. Railway provisions a Redis 7 instance automatically
3. Click the Redis service → **Variables** tab — note:
   ```
   REDIS_URL      (redis://:password@host:port)
   REDISHOST      (private hostname)
   REDISPORT
   REDISPASSWORD
   ```
4. Private hostname: `redis.railway.internal:6379`

**Equivalent to** `redis:` service in `compose.bunkerweb-1.6.9.yml`.

---

## Step 3 — Deploy the Spring Boot App

### 3a. Create `railway.toml` in the project root

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
startCommand = "java -jar /app/app.jar"
healthcheckPath = "/actuator/health"
healthcheckTimeout = 120
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 3
```

### 3b. Set Environment Variables

In Railway → your app service → **Variables**, add the following. Use Railway's variable references (`${{Service.VAR}}`) to link to the managed services:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8080` |
| `APP_BASE_URL` | `https://openmailer.site` |
| `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DATABASE_USERNAME` | `${{Postgres.PGUSER}}` |
| `DATABASE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `REDIS_HOST` | `${{Redis.REDISHOST}}` |
| `REDIS_PORT` | `${{Redis.REDISPORT}}` |
| `REDIS_PASSWORD` | `${{Redis.REDISPASSWORD}}` |
| `CACHE_TYPE` | `redis` |
| `ENCRYPTION_KEY` | *(generate: `openssl rand -hex 16`)* |
| `JWT_SECRET` | *(generate: `openssl rand -base64 32`)* |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `3600000` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` |
| `MAIL_HOST` | your SMTP host |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | your SMTP username |
| `MAIL_PASSWORD` | your SMTP password |
| `APP_MAIL_FROM` | your from address |
| `APP_ASSETS_MINIFIED` | `true` |

> **Security**: Never use the compose file's default secrets (`cd18b33b...`, `hbJkYj...`) in production. Generate new values.

### 3c. Connect repo and deploy

```bash
railway login
railway link          # link to your Railway project
railway up            # deploy from current directory
```

Or connect the GitHub repo in the Railway dashboard for automatic deploys on push.

---

## Step 4 — Custom Domain + Cloudflare SSL

Railway provides a generated domain (`*.up.railway.app`). To use `openmailer.site`:

1. Railway app service → **Settings → Networking → Custom Domain**
2. Add `openmailer.site` and `www.openmailer.site`
3. Railway shows a `CNAME` target — add this in Cloudflare DNS:
   ```
   Type    Name    Target                          Proxy
   CNAME   @       yourapp.up.railway.app          ✅ Proxied
   CNAME   www     yourapp.up.railway.app          ✅ Proxied
   ```
4. In Cloudflare SSL/TLS → set mode to **Full (strict)**
5. Railway handles the origin certificate; Cloudflare handles the public-facing cert

---

## Step 5 — Cloudflare Security (replaces BunkerWeb)

Cloudflare Free tier covers the core protections BunkerWeb provides:

| BunkerWeb Feature | Cloudflare Equivalent |
|---|---|
| WAF (bad bot blocking) | Security → WAF → Managed Rules |
| Rate limiting (`20r/s`, `10r/m`) | Security → Rate Limiting rules |
| Bad behavior banning | Security → Bot Fight Mode |
| DNSBL checks | Cloudflare's IP reputation lists (automatic) |
| Blacklist URIs (`/wp-admin`, `/.env`) | WAF → Custom Rules → URI path match |
| HTTP→HTTPS redirect | SSL/TLS → Always Use HTTPS |
| Security headers | Transform Rules → Response Header Modification |
| HSTS | SSL/TLS → Edge Certificates → HSTS |

### Recommended Cloudflare WAF Custom Rules

**Block common scanners** (matches the `BLACKLIST_URI` in compose):
```
(http.request.uri.path matches "^/wp-admin" or
 http.request.uri.path matches "^/wp-login\.php" or
 http.request.uri.path matches "^/xmlrpc\.php" or
 http.request.uri.path matches "^\/.env" or
 http.request.uri.path matches "^/server-status")
→ Action: Block
```

**Rate limit auth endpoint** (matches `LIMIT_REQ_RATE_2=10r/m`):
```
URI path starts with /api/auth
→ 10 requests per minute per IP → Action: Block
```

### Recommended Security Headers (Transform Rule)

Add these response headers to match the compose file's BunkerWeb config:

```
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: object-src 'none'; form-action 'self'; frame-ancestors 'self';
Permissions-Policy: accelerometer=(), autoplay=(), camera=(), display-capture=(),
  encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), magnetometer=(),
  microphone=(), midi=(), payment=(), picture-in-picture=(),
  publickey-credentials-get=(), screen-wake-lock=(), usb=(), xr-spatial-tracking=()
```

---

## Step 6 — Environment Checklist Before Go-Live

- [ ] `ENCRYPTION_KEY` — new 32-char hex, not the compose default
- [ ] `JWT_SECRET` — new base64 secret, not the compose default
- [ ] `DATABASE_PASSWORD` — Railway auto-generates a strong password
- [ ] `REDIS_PASSWORD` — Railway auto-generates
- [ ] SMTP credentials pointing to production provider (AWS SES / SendGrid), not Ethereal
- [ ] `APP_BASE_URL` set to production domain
- [ ] Cloudflare proxy enabled (orange cloud) on all DNS records
- [ ] Railway environment set to `prod` (`SPRING_PROFILES_ACTIVE=prod`)

---

## Optional: Hybrid Approach — BunkerWeb on VPS

If you need BunkerWeb's full WAF (ModSecurity rules, CrowdSec integration, BunkerNet threat intelligence) in front of the Railway-hosted app:

```
Internet
    │
    ▼
BunkerWeb (VPS — Hetzner CX22 ~€4/mo, DigitalOcean $6/mo)
├── bunkerity/bunkerweb:1.6.9
├── bunkerity/bunkerweb-scheduler:1.6.9
├── bunkerity/bunkerweb-autoconf:1.6.9
└── PostgreSQL (BunkerWeb config only)
    │
    ▼ REVERSE_PROXY_HOST
Railway App (openmailer.site.up.railway.app)
├── openmailer-app
├── Managed PostgreSQL
└── Managed Redis
```

### VPS BunkerWeb compose changes

On the VPS, use a stripped-down compose that only runs BunkerWeb services (no app, postgres, or redis). Change the app label to point to your Railway internal URL:

```yaml
# On VPS — bunkerweb points to Railway app
labels:
  - "bunkerweb.SERVER_NAME=openmailer.site"
  - "bunkerweb.USE_REVERSE_PROXY=yes"
  - "bunkerweb.REVERSE_PROXY_HOST=https://yourapp.up.railway.app"
  - "bunkerweb.REVERSE_PROXY_URL=/"
  - "bunkerweb.AUTO_LETS_ENCRYPT=yes"          # real cert on VPS
  - "bunkerweb.EMAIL_LETS_ENCRYPT=you@email.com"
  # ... all other security labels from compose.bunkerweb-1.6.9.yml
```

Point your DNS `A` record to the VPS IP (not Railway), so all traffic passes through BunkerWeb before reaching Railway. Set `REVERSE_PROXY_HOST` to the Railway-generated `*.up.railway.app` URL (Railway's internal origin, not the public domain).

---

## Railway vs Local Compose — Mapping

| `compose.bunkerweb-1.6.9.yml` service | Railway equivalent |
|---|---|
| `postgres` | Railway PostgreSQL plugin |
| `redis` | Railway Redis plugin |
| `app` | Railway service (Dockerfile build) |
| `bunkerweb` | Cloudflare WAF (or VPS hybrid) |
| `bw-scheduler` | Not needed (no BunkerWeb on Railway) |
| `bw-autoconf` | Not needed |
| `bw-ui` | Not needed (or deploy separately on VPS) |
| `bw-docker` | Not needed |

---

## Useful Railway CLI Commands

```bash
# View logs
railway logs

# Open shell in running service
railway shell

# Run one-off command (e.g. DB migration)
railway run ./mvnw flyway:migrate

# Check service status
railway status

# Set a variable
railway variables set KEY=value

# Link to specific service in project
railway service
```
