# DNS Configuration Guide for Email

**Purpose:** Complete reference for email-related DNS records
**Audience:** Anyone setting up email providers (SendGrid, AWS SES, Custom SMTP)
**Difficulty:** ⭐⭐ Moderate

---

## 📋 Table of Contents

1. [DNS Basics](#dns-basics)
2. [Email-Related DNS Records](#email-related-dns-records)
3. [SPF (Sender Policy Framework)](#spf-sender-policy-framework)
4. [DKIM (DomainKeys Identified Mail)](#dkim-domainkeys-identified-mail)
5. [DMARC (Domain-based Message Authentication)](#dmarc-domain-based-message-authentication)
6. [PTR Records (Reverse DNS)](#ptr-records-reverse-dns)
7. [Configuration Examples](#configuration-examples)
8. [Verification Tools](#verification-tools)
9. [Common Mistakes](#common-mistakes)
10. [Troubleshooting](#troubleshooting)

---

## 🌐 DNS Basics

### What is DNS?

**DNS (Domain Name System)** is like the internet's phone book. It translates human-readable domain names (like `google.com`) into IP addresses (like `142.250.80.46`) that computers use.

For email, DNS serves several critical purposes:
1. **Tell email servers where to send mail** (MX records)
2. **Prove you own the domain** (TXT records for SPF, DKIM, DMARC)
3. **Map IP addresses back to domains** (PTR records)

### DNS Record Types

| Type | Purpose | Example |
|------|---------|---------|
| **A** | Maps domain to IPv4 address | `mail.example.com` → `123.45.67.89` |
| **AAAA** | Maps domain to IPv6 address | `mail.example.com` → `2001:db8::1` |
| **MX** | Specifies mail servers for domain | `example.com` → `mail.example.com` |
| **CNAME** | Creates alias for another domain | `www.example.com` → `example.com` |
| **TXT** | Stores text information | SPF, DKIM, DMARC policies |
| **PTR** | Reverse DNS (IP to domain) | `123.45.67.89` → `mail.example.com` |

### DNS Propagation

When you add or change DNS records:
- **Minimum time:** 5-10 minutes
- **Average time:** 15-30 minutes
- **Maximum time:** 48-72 hours (rare)

**Why does it take time?**
- DNS servers worldwide cache records (TTL - Time To Live)
- Changes must propagate through multiple DNS servers
- Lower TTL = faster propagation, but more DNS queries

**TTL Values:**
- **300 seconds (5 minutes):** Fast changes, higher DNS load
- **3600 seconds (1 hour):** Good balance (recommended)
- **86400 seconds (24 hours):** Rarely need to change, lowest load

---

## 📧 Email-Related DNS Records

### 1. A Record (Address Record)

**Purpose:** Points your mail subdomain to your server's IP address

**When needed:**
- Custom SMTP server setup
- When running your own mail server

**Not needed for:**
- SendGrid (they host the server)
- AWS SES (Amazon hosts the server)

**Example:**
```
Type:  A
Name:  mail
Value: 123.45.67.89
TTL:   Auto (or 3600)
```

**Result:** `mail.example.com` points to IP `123.45.67.89`

**Cloudflare Proxy Setting:**
- ⚠️ **Must be Gray (DNS only)**
- Orange (Proxied) breaks email delivery!

---

### 2. MX Record (Mail Exchange)

**Purpose:** Tells email servers where to deliver mail for your domain

**When needed:**
- **Always** - if you want to receive email
- Optional if you only send email (never receive)

**Priority:**
- Lower number = higher priority
- If one server fails, tries next priority
- Single server: Use priority `10`
- Multiple servers: `10`, `20`, `30`, etc.

**Example:**
```
Type:     MX
Name:     @ (or leave blank for root domain)
Value:    mail.example.com
Priority: 10
TTL:      Auto (or 3600)
```

**Result:** Emails to `user@example.com` are delivered to `mail.example.com`

**Multiple Mail Servers (Advanced):**
```
Priority 10: mail1.example.com (primary)
Priority 20: mail2.example.com (backup)
Priority 30: mail3.example.com (backup)
```

---

### 3. CNAME Record (Canonical Name)

**Purpose:** Creates an alias pointing to another domain

**When needed:**
- SendGrid domain verification (3 CNAMEs)
- AWS SES DKIM verification (3 CNAMEs)

**Example:**
```
Type:  CNAME
Name:  em1234
Value: u1234567.wl001.sendgrid.net
TTL:   Auto
```

**Result:** `em1234.example.com` is an alias for `u1234567.wl001.sendgrid.net`

**⚠️ Important Rules:**
1. **Cannot** create CNAME for root domain (`@`)
2. **Must** have trailing dot in some DNS providers: `sendgrid.net.`
3. **Must** be gray (DNS only) in Cloudflare - orange breaks it!

---

### 4. TXT Record (Text Record)

**Purpose:** Stores arbitrary text data, used for email authentication

**Used for:**
- SPF (Sender Policy Framework)
- DKIM (DomainKeys Identified Mail)
- DMARC (Domain-based Message Authentication)
- Domain verification

**Example:**
```
Type:  TXT
Name:  @
Value: v=spf1 include:_spf.google.com ~all
TTL:   Auto
```

**⚠️ Important:**
- Value must be in **quotes** in some DNS providers
- One domain can have **multiple TXT records**
- Each TXT record has different name/purpose

---

## 🔐 SPF (Sender Policy Framework)

### What is SPF?

**SPF** tells receiving email servers which servers are **allowed** to send email from your domain.

**Purpose:**
- Prevents email spoofing (fake sender addresses)
- Improves deliverability
- Required by Gmail, Outlook for good inbox placement

### SPF Record Format

```
v=spf1 <mechanisms> <qualifier>
```

**Components:**

1. **v=spf1** - Version (always spf1)
2. **Mechanisms** - Which servers can send
3. **Qualifier** - What to do with failures

### SPF Mechanisms

| Mechanism | Meaning | Example |
|-----------|---------|---------|
| `a` | Servers listed in A record | Domain's A record |
| `mx` | Servers listed in MX record | Domain's mail servers |
| `ip4:` | Specific IPv4 address | `ip4:123.45.67.89` |
| `ip6:` | Specific IPv6 address | `ip6:2001:db8::1` |
| `include:` | Include another domain's SPF | `include:_spf.google.com` |
| `all` | All other servers | Always last |

### SPF Qualifiers

| Qualifier | Symbol | Meaning |
|-----------|--------|---------|
| Pass | `+` | Authorized (default if omitted) |
| Fail | `-` | Not authorized, reject |
| SoftFail | `~` | Probably not authorized, mark as suspicious |
| Neutral | `?` | No statement (not recommended) |

### SPF Examples

#### SendGrid Only:
```
v=spf1 include:sendgrid.net ~all
```
- Allow SendGrid servers
- SoftFail all others

#### AWS SES Only:
```
v=spf1 include:amazonses.com ~all
```
- Allow AWS SES servers
- SoftFail all others

#### Custom SMTP Server:
```
v=spf1 mx a ip4:123.45.67.89 ~all
```
- Allow MX record servers
- Allow A record servers
- Allow specific IP
- SoftFail all others

#### Multiple Providers:
```
v=spf1 include:sendgrid.net include:amazonses.com include:_spf.google.com ~all
```
- Allow SendGrid, AWS SES, and Google Workspace
- SoftFail all others

### SPF Best Practices

1. **Always end with `~all` or `-all`**
   - `~all` (SoftFail) - recommended, less strict
   - `-all` (Fail) - stricter, may cause issues

2. **Limit DNS lookups to 10**
   - Each `include:` counts as 1 lookup
   - Each `mx` counts as 1 lookup
   - Exceeding 10 causes SPF to fail

3. **Use `include:` for third-party senders**
   - Don't duplicate their SPF, reference it

4. **One SPF record per domain**
   - Multiple SPF records = all fail
   - Combine into one record with multiple mechanisms

### SPF Record in Cloudflare

```
Type:  TXT
Name:  @ (root domain)
Value: v=spf1 include:sendgrid.net ~all
TTL:   Auto
```

---

## ✉️ DKIM (DomainKeys Identified Mail)

### What is DKIM?

**DKIM** adds a **digital signature** to outgoing emails to prove they weren't modified in transit and came from your domain.

**How it works:**
1. Your mail server signs each email with a private key
2. DNS publishes the corresponding public key
3. Receiving server verifies signature using public key
4. If valid, email is trustworthy

**Benefits:**
- Proves email authenticity
- Prevents email tampering
- Improves deliverability
- Required for good inbox placement

### DKIM Record Format

```
v=DKIM1; k=rsa; p=<public-key>
```

**Components:**

| Component | Meaning |
|-----------|---------|
| `v=DKIM1` | DKIM version |
| `k=rsa` | Key type (RSA encryption) |
| `p=` | Public key (long base64 string) |
| `h=sha256` | Hash algorithm (optional) |
| `s=email` | Service type (optional) |

### DKIM Selector

DKIM records use a **selector** to support multiple keys:

```
<selector>._domainkey.example.com
```

**Examples:**
- `default._domainkey.example.com`
- `s1._domainkey.example.com`
- `google._domainkey.example.com`

**Why selectors?**
- Rotate keys without downtime
- Different keys for different email sources
- Easier key management

### DKIM Examples

#### SendGrid (2 keys):
```
Type:  CNAME
Name:  s1._domainkey
Value: s1.domainkey.u1234567.wl001.sendgrid.net

Type:  CNAME
Name:  s2._domainkey
Value: s2.domainkey.u1234567.wl001.sendgrid.net
```

#### AWS SES (3 keys):
```
Type:  CNAME
Name:  abc123._domainkey
Value: abc123.dkim.amazonses.com

Type:  CNAME
Name:  def456._domainkey
Value: def456.dkim.amazonses.com

Type:  CNAME
Name:  ghi789._domainkey
Value: ghi789.dkim.amazonses.com
```

#### Custom SMTP Server:
```
Type:  TXT
Name:  default._domainkey
Value: v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```

### DKIM Best Practices

1. **Use 2048-bit keys minimum**
   - 1024-bit is weak, deprecated
   - 2048-bit is standard
   - 4096-bit may have compatibility issues

2. **Rotate keys annually**
   - Generate new key
   - Add to DNS
   - Update mail server
   - Remove old key after 1 week

3. **Use multiple selectors**
   - Allows rotation without downtime
   - Different keys for different sources

4. **Monitor DKIM failures**
   - Set up DMARC reporting
   - Fix issues immediately

### DKIM Record in Cloudflare

```
Type:  TXT
Name:  default._domainkey
Value: v=DKIM1; h=sha256; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA<very-long-string>
TTL:   Auto
Proxy: Gray (DNS only)
```

**⚠️ Important:**
- Remove line breaks from public key
- Cloudflare may not need quotes, some providers do
- Must be gray (DNS only), not orange!

---

## 🛡️ DMARC (Domain-based Message Authentication)

### What is DMARC?

**DMARC** tells receiving servers what to do with emails that **fail** SPF or DKIM checks.

**Purpose:**
- Enforces SPF and DKIM policies
- Provides reporting on email authentication
- Prevents domain spoofing
- Improves brand protection

**Requirements:**
- Must have SPF record
- Must have DKIM record
- DMARC builds on top of both

### DMARC Record Format

```
v=DMARC1; p=<policy>; rua=mailto:<email>; ruf=mailto:<email>; pct=<percentage>
```

**Components:**

| Tag | Meaning | Values |
|-----|---------|--------|
| `v=DMARC1` | Version (required) | Always DMARC1 |
| `p=` | Policy (required) | `none`, `quarantine`, `reject` |
| `rua=` | Aggregate reports | Email address |
| `ruf=` | Forensic reports | Email address |
| `pct=` | Percentage to enforce | 0-100 (default: 100) |
| `adkim=` | DKIM alignment | `r` (relaxed), `s` (strict) |
| `aspf=` | SPF alignment | `r` (relaxed), `s` (strict) |
| `sp=` | Subdomain policy | `none`, `quarantine`, `reject` |

### DMARC Policies

| Policy | Action | When to Use |
|--------|--------|-------------|
| `p=none` | Monitor only, don't reject | Testing phase, getting started |
| `p=quarantine` | Mark as spam if failed | Recommended for production |
| `p=reject` | Reject email if failed | Maximum protection, after testing |

### DMARC Implementation Phases

#### Phase 1: Monitoring (p=none)
```
v=DMARC1; p=none; rua=mailto:dmarc@example.com; pct=100
```
- Don't reject any emails
- Collect reports to identify issues
- Run for 1-2 weeks

#### Phase 2: Quarantine (p=quarantine)
```
v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com; pct=100; adkim=r; aspf=r
```
- Failed emails go to spam
- Still getting reports
- Recommended for most users

#### Phase 3: Reject (p=reject)
```
v=DMARC1; p=reject; rua=mailto:dmarc@example.com; pct=100; adkim=s; aspf=s
```
- Failed emails are rejected completely
- Strictest policy
- Only after confirming everything works

### DMARC Alignment

**Alignment** means SPF/DKIM domain must match From: header domain.

#### Relaxed Alignment (`r`)
- Subdomains count as aligned
- `mail.example.com` aligns with `example.com`
- Easier, recommended for most

#### Strict Alignment (`s`)
- Must match exactly
- `mail.example.com` does NOT align with `example.com`
- More secure, but can cause issues

### DMARC Examples

#### Basic (Monitoring):
```
v=DMARC1; p=none; rua=mailto:dmarc@example.com
```

#### Recommended (Production):
```
v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com; pct=100; adkim=r; aspf=r
```

#### Strict (Maximum Protection):
```
v=DMARC1; p=reject; rua=mailto:dmarc@example.com; ruf=mailto:dmarc-forensic@example.com; pct=100; adkim=s; aspf=s
```

### DMARC Record in Cloudflare

```
Type:  TXT
Name:  _dmarc
Value: v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com; pct=100; adkim=r; aspf=r
TTL:   Auto
```

### DMARC Reporting

**Aggregate Reports (rua):**
- Sent daily by receiving servers
- XML format
- Summary of authentication results

**Forensic Reports (ruf):**
- Sent when emails fail
- Individual email details
- Can be high volume

**Where to send reports:**
- Your own email: `dmarc@yourdomain.com`
- Third-party service: [Postmark DMARC](https://dmarc.postmarkapp.com/), [DMARC Analyzer](https://www.dmarcanalyzer.com/)

### DMARC Best Practices

1. **Start with `p=none`**
   - Monitor for 1-2 weeks
   - Fix any SPF/DKIM issues
   - Then move to `p=quarantine`

2. **Always set `rua` for reports**
   - Essential for debugging
   - Use third-party service if you don't want to parse XML

3. **Use `pct=` for gradual rollout**
   - Start with `pct=10` (10% of emails)
   - Gradually increase to 100%
   - Safer for large senders

4. **Set subdomain policy**
   - `sp=quarantine` or `sp=reject`
   - Protects subdomains too

---

## 🔄 PTR Records (Reverse DNS)

### What is a PTR Record?

**PTR (Pointer)** record maps an IP address back to a domain name (opposite of A record).

**Normal DNS:** `mail.example.com` → `123.45.67.89`
**Reverse DNS:** `123.45.67.89` → `mail.example.com`

### Why PTR Records Matter

Email servers use PTR records to verify you own the IP address:

1. Email server sees email from `123.45.67.89`
2. Looks up PTR record for that IP
3. Should return `mail.example.com`
4. Verifies this matches the HELO hostname
5. If match = legitimate, if not = spam

**Without PTR record:**
- Emails often rejected or marked as spam
- Many servers won't accept emails at all
- Critical for custom SMTP servers

### Who Controls PTR Records?

**Not you!** PTR records are controlled by whoever owns the IP address:

- **VPS providers:** DigitalOcean, Linode, Hetzner
- **Cloud providers:** AWS, Google Cloud, Azure
- **ISPs:** AT&T, Comcast, etc.

### How to Set PTR Record

#### For VPS (DigitalOcean, Linode, Vultr):

1. Go to your VPS provider's control panel
2. Find your droplet/server
3. Look for "Networking" or "Reverse DNS" section
4. Enter: `mail.yourdomain.com`
5. Save

#### For AWS EC2:

1. AWS doesn't allow customer-set PTR by default
2. Submit request: [AWS Reverse DNS Request Form](https://aws.amazon.com/forms/ec2-email-limit-rdns-request)
3. Wait for approval (24-48 hours)

#### For Dedicated Servers:

Contact your hosting provider's support team.

### Verify PTR Record

```bash
# Check PTR record
nslookup 123.45.67.89

# Should return:
89.67.45.123.in-addr.arpa    name = mail.example.com

# Or use dig:
dig -x 123.45.67.89

# Or use online tool:
# https://mxtoolbox.com/ReverseLookup.aspx
```

### PTR Record Best Practices

1. **Must match HELO hostname**
   - PTR: `mail.example.com`
   - HELO: `mail.example.com`
   - Must be identical!

2. **Set before sending email**
   - Some email servers cache results
   - Set PTR, wait 24 hours, then send

3. **Don't use shared IP for email**
   - Shared hosting IPs often have bad reputation
   - Get dedicated IP for email server

4. **Check regularly**
   - If you change VPS, PTR resets
   - Verify after any infrastructure changes

---

## 📝 Configuration Examples

### Complete DNS Setup for SendGrid

```
# Domain verification (provided by SendGrid)
Type: CNAME | Name: em1234           | Value: u1234567.wl001.sendgrid.net
Type: CNAME | Name: s1._domainkey    | Value: s1.domainkey.u1234567.wl001.sendgrid.net
Type: CNAME | Name: s2._domainkey    | Value: s2.domainkey.u1234567.wl001.sendgrid.net

# SPF (your configuration)
Type: TXT   | Name: @                | Value: v=spf1 include:sendgrid.net ~all

# DMARC (your configuration)
Type: TXT   | Name: _dmarc           | Value: v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com
```

### Complete DNS Setup for AWS SES

```
# DKIM keys (provided by AWS SES)
Type: CNAME | Name: abc123._domainkey | Value: abc123.dkim.amazonses.com
Type: CNAME | Name: def456._domainkey | Value: def456.dkim.amazonses.com
Type: CNAME | Name: ghi789._domainkey | Value: ghi789.dkim.amazonses.com

# SPF (your configuration)
Type: TXT   | Name: @                 | Value: v=spf1 include:amazonses.com ~all

# DMARC (your configuration)
Type: TXT   | Name: _dmarc            | Value: v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com

# MX record (optional, for receiving bounces)
Type: MX    | Name: @                 | Value: feedback-smtp.us-east-1.amazonses.com | Priority: 10
```

### Complete DNS Setup for Custom SMTP

```
# A record (points to your server)
Type: A     | Name: mail              | Value: 123.45.67.89

# MX record (for receiving email)
Type: MX    | Name: @                 | Value: mail.example.com | Priority: 10

# SPF (authorizes your server)
Type: TXT   | Name: @                 | Value: v=spf1 mx a ip4:123.45.67.89 ~all

# DKIM (your generated key)
Type: TXT   | Name: default._domainkey | Value: v=DKIM1; k=rsa; p=MIIBIjANBgkqhk...

# DMARC (your policy)
Type: TXT   | Name: _dmarc            | Value: v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com; adkim=r; aspf=r

# PTR record (set in VPS provider, not DNS)
# 123.45.67.89 → mail.example.com
```

---

## 🔍 Verification Tools

### 1. Check DNS Propagation

**WhatsMy DNS:**
- URL: [https://www.whatsmydns.net/](https://www.whatsmydns.net/)
- Shows DNS from multiple locations worldwide
- Good for checking propagation status

**DNS Checker:**
- URL: [https://dnschecker.org/](https://dnschecker.org/)
- Similar to WhatsMy DNS
- Shows more locations

### 2. Check SPF Record

**SPF Record Checker:**
- URL: [https://mxtoolbox.com/spf.aspx](https://mxtoolbox.com/spf.aspx)
- Validates SPF syntax
- Checks DNS lookup count
- Shows warnings

**Command line:**
```bash
nslookup -type=TXT example.com
dig TXT example.com
```

### 3. Check DKIM Record

**DKIM Validator:**
- URL: [https://mxtoolbox.com/dkim.aspx](https://mxtoolbox.com/dkim.aspx)
- Enter domain and selector
- Validates public key

**Command line:**
```bash
nslookup -type=TXT default._domainkey.example.com
dig TXT default._domainkey.example.com
```

### 4. Check DMARC Record

**DMARC Checker:**
- URL: [https://mxtoolbox.com/dmarc.aspx](https://mxtoolbox.com/dmarc.aspx)
- Validates DMARC syntax
- Explains policy

**Command line:**
```bash
nslookup -type=TXT _dmarc.example.com
dig TXT _dmarc.example.com
```

### 5. Check PTR Record

**Reverse DNS Lookup:**
- URL: [https://mxtoolbox.com/ReverseLookup.aspx](https://mxtoolbox.com/ReverseLookup.aspx)
- Enter IP address
- Shows PTR record

**Command line:**
```bash
nslookup 123.45.67.89
dig -x 123.45.67.89
```

### 6. Complete Email Authentication Check

**Mail Tester:**
- URL: [https://www.mail-tester.com/](https://www.mail-tester.com/)
- Send email to provided address
- Get score out of 10
- Shows all authentication results

**Google Admin Toolbox:**
- URL: [https://toolbox.googleapps.com/apps/checkmx/](https://toolbox.googleapps.com/apps/checkmx/)
- Check MX records
- Verify email server configuration

---

## ⚠️ Common Mistakes

### 1. Orange Cloud in Cloudflare

**Mistake:**
- Leaving Cloudflare proxy (orange cloud) enabled for email records

**Why it's wrong:**
- Cloudflare proxy only works for HTTP/HTTPS
- Email uses SMTP protocol
- Proxy breaks DKIM, MX, and mail delivery

**Solution:**
- Click orange cloud to make it gray (DNS only)
- All email-related records must be gray

### 2. Multiple SPF Records

**Mistake:**
```
v=spf1 include:sendgrid.net ~all
v=spf1 include:amazonses.com ~all
```

**Why it's wrong:**
- Multiple SPF records = **all SPF fails**
- Only one SPF record allowed per domain

**Solution:**
- Combine into one record:
```
v=spf1 include:sendgrid.net include:amazonses.com ~all
```

### 3. Missing `~all` or `-all`

**Mistake:**
```
v=spf1 include:sendgrid.net
```

**Why it's wrong:**
- No policy for unauthorized servers
- Default becomes `+all` (allow all) - defeats purpose

**Solution:**
```
v=spf1 include:sendgrid.net ~all
```

### 4. DKIM Record Line Breaks

**Mistake:**
```
v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
abcdefghijklmnopqrstuvwxyz1234567890
```

**Why it's wrong:**
- Some DNS providers break the key
- Line breaks invalidate the key

**Solution:**
- Remove all line breaks:
```
v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQabcdefghijklmnopqrstuvwxyz1234567890
```

### 5. Wrong DMARC Alignment

**Mistake:**
- Setting `adkim=s` (strict) when using subdomain

**Why it's wrong:**
- If you send from `mail.example.com` but From: header is `user@example.com`
- Strict alignment requires exact match
- Emails fail DMARC

**Solution:**
- Use `adkim=r` (relaxed) - allows subdomains

### 6. No PTR Record

**Mistake:**
- Setting up custom SMTP without configuring PTR

**Why it's wrong:**
- Most email servers require PTR record
- Emails rejected or marked as spam
- Gmail often refuses emails without PTR

**Solution:**
- Set PTR in VPS provider before sending emails
- Verify with `nslookup YOUR_IP`

### 7. Forgetting to Verify Domain

**Mistake:**
- Adding DNS records but not clicking "Verify" in provider

**Why it's wrong:**
- Provider doesn't know verification is complete
- Can't send emails yet

**Solution:**
- After adding DNS records, click "Verify" button
- Wait for "Verified" status before sending

---

## 🔧 Troubleshooting

### DNS Records Not Found

**Symptoms:**
- `nslookup` returns "Non-existent domain"
- Provider says "DNS records not found"

**Solutions:**

1. **Wait for propagation:**
   ```bash
   # Check if records exist at authoritative server
   dig @1.1.1.1 TXT example.com
   # or
   dig @8.8.8.8 TXT example.com
   ```

2. **Verify record name:**
   - `@` means root domain
   - `_dmarc` means `_dmarc.example.com`
   - `default._domainkey` means `default._domainkey.example.com`

3. **Check for typos:**
   - Compare provider's required value character-by-character
   - One wrong character = failure

4. **Verify domain ownership:**
   - Are you logged into correct account?
   - Do you actually own this domain?

### SPF "Too Many DNS Lookups"

**Symptoms:**
- SPF record fails
- Error: "Too many DNS lookups (11+)"

**Cause:**
- SPF spec limits lookups to 10
- Each `include:` or `mx` counts as lookup

**Solutions:**

1. **Reduce includes:**
   ```bash
   # Before (11 lookups):
   v=spf1 include:spf1.com include:spf2.com include:spf3.com mx a ~all

   # After (optimize):
   v=spf1 include:spf1.com include:spf2.com ip4:1.2.3.4 ~all
   ```

2. **Use IP addresses:**
   - `ip4:1.2.3.4` = 0 lookups
   - `include:example.com` = 1 lookup
   - Replace includes with IPs when possible

3. **Remove unnecessary mechanisms:**
   - Do you really need `mx` if you never send from mail servers?
   - Remove what you don't use

### DKIM Verification Fails

**Symptoms:**
- Provider says "Invalid DKIM record"
- Emails fail DKIM checks

**Solutions:**

1. **Check selector:**
   ```bash
   # Verify selector matches what provider expects
   dig TXT s1._domainkey.example.com
   ```

2. **Remove line breaks:**
   - Public key must be one continuous string
   - No spaces, line breaks, or formatting

3. **Check for quotes:**
   - Some DNS providers require quotes: `"v=DKIM1..."`
   - Some don't: `v=DKIM1...`
   - Try both

4. **Regenerate key:**
   - Key might be corrupted
   - Generate new key pair
   - Update DNS and mail server

### DMARC Not Working

**Symptoms:**
- DMARC record found but policy not enforced
- Not receiving DMARC reports

**Solutions:**

1. **Check SPF and DKIM first:**
   - DMARC requires at least one to pass
   - Fix SPF or DKIM before troubleshooting DMARC

2. **Verify record syntax:**
   ```bash
   dig TXT _dmarc.example.com
   ```
   - Must start with `v=DMARC1;`
   - Must have `p=` policy
   - Check for typos

3. **Wait for reports:**
   - Aggregate reports sent daily
   - May take 24-48 hours to receive first report
   - Check spam folder

4. **Use DMARC analyzer:**
   - [DMARC Analyzer](https://www.dmarcanalyzer.com/)
   - Receives and parses reports for you

### Emails Still Going to Spam

**Symptoms:**
- All DNS records correct
- Still going to spam folder

**Solutions:**

1. **Build IP reputation:**
   - New IPs have no reputation
   - Start slow: 10-20 emails/day
   - Gradually increase over 2-4 weeks

2. **Improve email content:**
   - Avoid spam trigger words
   - Include plain text version
   - Add unsubscribe link
   - Don't use URL shorteners

3. **Warm up domain:**
   - Send to engaged users first
   - Ask them to mark as "Not Spam"
   - Build positive engagement

4. **Check blacklists:**
   - [MXToolbox Blacklist Check](https://mxtoolbox.com/blacklists.aspx)
   - If listed, request delisting

---

## 📚 Summary Checklist

Before sending emails, verify:

- [ ] **A Record** (custom SMTP only)
  - Points `mail.example.com` to server IP
  - Gray (DNS only) in Cloudflare

- [ ] **MX Record** (if receiving email)
  - Points to mail server
  - Priority set (usually 10)

- [ ] **SPF Record**
  - One record only (no duplicates)
  - Ends with `~all` or `-all`
  - Less than 10 DNS lookups

- [ ] **DKIM Records**
  - All selectors configured
  - Public key has no line breaks
  - Gray (DNS only) in Cloudflare

- [ ] **DMARC Record**
  - Name: `_dmarc`
  - Policy set (`p=quarantine` recommended)
  - Reporting email configured

- [ ] **PTR Record** (custom SMTP only)
  - Set in VPS provider (not DNS)
  - Matches mail server hostname

- [ ] **Verification**
  - All records verified with `nslookup`/`dig`
  - Mail-tester.com score 8/10+
  - Test emails delivered successfully

---

**This guide is comprehensive - bookmark it for reference!**

**Quick links:**
- [SPF Checker](https://mxtoolbox.com/spf.aspx)
- [DKIM Checker](https://mxtoolbox.com/dkim.aspx)
- [DMARC Checker](https://mxtoolbox.com/dmarc.aspx)
- [Mail Tester](https://www.mail-tester.com/)
- [DNS Propagation](https://www.whatsmydns.net/)

---

**Need help?** Check the specific setup guide for your provider:
- [SendGrid Setup Guide](EMAIL_SETUP_SENDGRID.md)
- [AWS SES Setup Guide](EMAIL_SETUP_AWS_SES.md)
- [Custom SMTP Setup Guide](EMAIL_SETUP_SMTP.md)
