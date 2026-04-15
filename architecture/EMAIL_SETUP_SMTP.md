# Custom SMTP Server Setup Guide

**Difficulty:** ⭐⭐⭐⭐ Advanced
**Estimated Time:** 8-12 hours (first time), 4-6 hours (experienced)
**Cost:** $5-20/month (VPS hosting)
**Best For:** Learning email infrastructure, full control, no per-email costs

---

## ⚠️ Important Warning

Setting up your own mail server is **significantly more complex** than using SendGrid or AWS SES. Consider carefully:

**Pros:**
- ✅ Deep understanding of email infrastructure
- ✅ Full control over email delivery
- ✅ No per-email costs after setup
- ✅ Privacy (your data stays on your server)
- ✅ Great learning experience

**Cons:**
- ❌ Time-consuming setup and maintenance
- ❌ Requires Linux server administration skills
- ❌ IP reputation building takes weeks/months
- ❌ Can be blocked by Gmail/Outlook if misconfigured
- ❌ Ongoing security and updates required
- ❌ Deliverability often worse than commercial providers

**Recommendation:**
- **For production apps:** Use AWS SES or SendGrid
- **For learning:** Follow this guide!
- **Hybrid approach:** Use this for learning, keep AWS SES for production backup

---

## 📋 Prerequisites

### Required Skills:
- [ ] Basic Linux command line knowledge
- [ ] SSH access and usage
- [ ] Text editor usage (nano, vim, or vi)
- [ ] DNS concepts understanding
- [ ] Patience and troubleshooting skills

### Required Resources:
- [ ] VPS or dedicated server (2GB RAM minimum, 4GB recommended)
- [ ] Custom domain with DNS access
- [ ] Cloudflare account (or other DNS provider)
- [ ] Credit card for VPS provider
- [ ] 8-12 hours of uninterrupted time

### Recommended VPS Providers:
1. **DigitalOcean** - $6/month, good documentation, easy to use
2. **Hetzner** - €4.51/month, great value, EU-based
3. **Linode** - $5/month, reliable, good support
4. **Vultr** - $6/month, many locations
5. **AWS EC2** - Variable cost, free tier available for 1 year

---

## 🎯 What We'll Build

```
┌─────────────────────────────────────────────────────────────┐
│                    Your Mail Server                         │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │   Postfix    │  │   Dovecot    │  │   OpenDKIM      │  │
│  │ (SMTP Server)│  │ (SASL Auth)  │  │ (Email Signing) │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐                       │
│  │ Let's Encrypt│  │   Fail2ban   │                       │
│  │  (TLS/SSL)   │  │  (Security)  │                       │
│  └──────────────┘  └──────────────┘                       │
└─────────────────────────────────────────────────────────────┘
         │                                 │
         │                                 │
         ▼                                 ▼
   DNS Records                        Port 587 (TLS)
   (SPF, DKIM,                        Port 465 (SSL)
    DMARC, PTR)                       Port 25 (blocked)
```

---

## 🚀 Part 1: VPS Setup

### Step 1.1: Create VPS Instance

Using DigitalOcean as example (similar for other providers):

1. Go to [https://www.digitalocean.com/](https://www.digitalocean.com/)
2. Create account and add payment method
3. Click **"Create" → "Droplets"**
4. Select configuration:
   - **Distribution:** Ubuntu 22.04 LTS (recommended)
   - **Plan:** Basic
   - **CPU:** Regular (2GB RAM minimum, 4GB recommended)
   - **Datacenter:** Choose closest to your users
   - **Authentication:** SSH keys (recommended) or password
   - **Hostname:** `mail.yourdomain.com`
5. Click **"Create Droplet"**
6. Wait 55 seconds for creation
7. Copy the IP address

**Your server IP:** `123.456.789.012` (example)

### Step 1.2: Configure Reverse DNS (PTR Record)

⚠️ **CRITICAL:** This step is essential! Without proper PTR record, your emails will be marked as spam.

**What is PTR?**
- PTR (Pointer) record maps IP address → domain name
- Opposite of A record (domain → IP)
- Email servers check this to verify legitimacy

**How to set PTR record:**

1. In your VPS provider dashboard (DigitalOcean, Hetzner, etc.):
   - Find "Networking" or "PTR Records" section
   - Click your droplet/server
   - Find "Reverse DNS" or "PTR Record" option
   - Set to: `mail.yourdomain.com`
   - Save

2. **Verify PTR record:**
```bash
# From your local machine
nslookup 123.456.789.012

# Should return:
# 12.789.456.123.in-addr.arpa  name = mail.yourdomain.com
```

**If your VPS provider doesn't support PTR records:**
- Contact support to request it
- OR choose a different VPS provider (essential for mail servers!)

---

## 🚀 Part 2: Initial Server Configuration

### Step 2.1: Connect to Server

```bash
# Replace with your actual IP address
ssh root@123.456.789.012
```

Enter password or use SSH key.

### Step 2.2: Update System

```bash
# Update package lists
apt update

# Upgrade all packages
apt upgrade -y

# Install essential tools
apt install -y curl wget vim nano ufw software-properties-common
```

### Step 2.3: Set Hostname

```bash
# Set the hostname
hostnamectl set-hostname mail.yourdomain.com

# Verify
hostnamectl
# Should show: Static hostname: mail.yourdomain.com

# Update /etc/hosts
nano /etc/hosts
```

Add these lines at the top (replace IP and domain):
```
127.0.0.1 localhost
123.456.789.012 mail.yourdomain.com mail

# IPv6 if applicable
::1 localhost ip6-localhost ip6-loopback
```

Save (Ctrl+O, Enter) and exit (Ctrl+X).

### Step 2.4: Configure Firewall

```bash
# Allow SSH (CRITICAL! Do this first or you'll lock yourself out)
ufw allow 22/tcp

# Allow HTTP (for Let's Encrypt certificate validation)
ufw allow 80/tcp

# Allow HTTPS (optional, if you'll run web server)
ufw allow 443/tcp

# Allow SMTP submission (TLS)
ufw allow 587/tcp

# Allow SMTPS (SSL)
ufw allow 465/tcp

# Enable firewall
ufw enable

# Check status
ufw status
```

**Expected output:**
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                    ALLOW       Anywhere
587/tcp                    ALLOW       Anywhere
465/tcp                    ALLOW       Anywhere
```

---

## 🚀 Part 3: Install Postfix

Postfix is the SMTP server that sends emails.

### Step 3.1: Install Postfix

```bash
# Install Postfix and SPF policy checker
apt install -y postfix postfix-policyd-spf-python

# During installation, you'll see a configuration wizard:
# 1. Select: "Internet Site"
# 2. System mail name: yourdomain.com (NOT mail.yourdomain.com)
```

### Step 3.2: Configure Postfix

Edit the main configuration file:

```bash
nano /etc/postfix/main.cf
```

Replace entire contents with this configuration (adjust domain/hostname):

```conf
# Basic Settings
smtpd_banner = $myhostname ESMTP $mail_name
biff = no
append_dot_mydomain = no
readme_directory = no
compatibility_level = 2

# TLS parameters (we'll add certificates later)
smtpd_tls_cert_file=/etc/letsencrypt/live/mail.yourdomain.com/fullchain.pem
smtpd_tls_key_file=/etc/letsencrypt/live/mail.yourdomain.com/privkey.pem
smtpd_tls_security_level=may
smtpd_tls_auth_only = yes
smtpd_tls_loglevel = 1
smtpd_tls_received_header = yes
smtpd_tls_session_cache_database = btree:${data_directory}/smtpd_scache

smtp_tls_security_level=may
smtp_tls_loglevel = 1
smtp_tls_session_cache_database = btree:${data_directory}/smtp_scache

# SMTP Authentication (via Dovecot SASL)
smtpd_sasl_type = dovecot
smtpd_sasl_path = private/auth
smtpd_sasl_auth_enable = yes
smtpd_sasl_security_options = noanonymous
smtpd_sasl_local_domain = $myhostname
broken_sasl_auth_clients = yes

# Mail queue settings
maximal_queue_lifetime = 1h
bounce_queue_lifetime = 1h

# Network settings
myhostname = mail.yourdomain.com
myorigin = $mydomain
mydomain = yourdomain.com
mydestination = $myhostname, localhost.$mydomain, localhost
relayhost =
mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128
mailbox_size_limit = 0
recipient_delimiter = +
inet_interfaces = all
inet_protocols = all

# Virtual alias support
virtual_alias_domains =
virtual_alias_maps = hash:/etc/postfix/virtual

# Restrictions to prevent spam
smtpd_helo_required = yes
smtpd_helo_restrictions =
    permit_mynetworks,
    permit_sasl_authenticated,
    reject_invalid_helo_hostname,
    reject_non_fqdn_helo_hostname,
    reject_unknown_helo_hostname

smtpd_sender_restrictions =
    permit_mynetworks,
    permit_sasl_authenticated,
    reject_non_fqdn_sender,
    reject_unknown_sender_domain

smtpd_recipient_restrictions =
    permit_mynetworks,
    permit_sasl_authenticated,
    reject_non_fqdn_recipient,
    reject_unknown_recipient_domain,
    reject_unauth_destination,
    check_policy_service unix:private/policyd-spf

# SPF Policy
policyd-spf_time_limit = 3600

# Milter configuration (for OpenDKIM)
milter_protocol = 6
milter_default_action = accept
smtpd_milters = inet:localhost:8891
non_smtpd_milters = inet:localhost:8891

# Disable VRFY command
disable_vrfy_command = yes

# Message size limit (50MB)
message_size_limit = 51200000
```

Save and exit.

### Step 3.3: Configure Master Settings

Edit master.cf to enable submission port:

```bash
nano /etc/postfix/master.cf
```

Find the `submission` section (around line 17-30) and uncomment/modify it to look like this:

```conf
submission inet n       -       y       -       -       smtpd
  -o syslog_name=postfix/submission
  -o smtpd_tls_security_level=encrypt
  -o smtpd_sasl_auth_enable=yes
  -o smtpd_tls_auth_only=yes
  -o smtpd_reject_unlisted_recipient=no
  -o smtpd_client_restrictions=permit_sasl_authenticated,reject
  -o smtpd_helo_restrictions=
  -o smtpd_sender_restrictions=
  -o smtpd_recipient_restrictions=
  -o smtpd_relay_restrictions=permit_sasl_authenticated,reject
  -o milter_macro_daemon_name=ORIGINATING

smtps     inet  n       -       y       -       -       smtpd
  -o syslog_name=postfix/smtps
  -o smtpd_tls_wrappermode=yes
  -o smtpd_sasl_auth_enable=yes
  -o smtpd_reject_unlisted_recipient=no
  -o smtpd_client_restrictions=permit_sasl_authenticated,reject
  -o smtpd_helo_restrictions=
  -o smtpd_sender_restrictions=
  -o smtpd_recipient_restrictions=
  -o smtpd_relay_restrictions=permit_sasl_authenticated,reject
  -o milter_macro_daemon_name=ORIGINATING
```

Also add SPF policy service at the end of the file:

```conf
policyd-spf  unix  -       n       n       -       0       spawn
    user=policyd-spf argv=/usr/bin/policyd-spf
```

Save and exit.

**Don't restart Postfix yet** - we need to set up Dovecot first.

---

## 🚀 Part 4: Install and Configure Dovecot

Dovecot provides SASL authentication for Postfix.

### Step 4.1: Install Dovecot

```bash
apt install -y dovecot-core dovecot-imapd dovecot-pop3d
```

### Step 4.2: Configure Dovecot Auth

```bash
nano /etc/dovecot/conf.d/10-master.conf
```

Find the `service auth` section and modify it:

```conf
service auth {
  # Postfix smtp-auth
  unix_listener /var/spool/postfix/private/auth {
    mode = 0660
    user = postfix
    group = postfix
  }
}
```

Save and exit.

### Step 4.3: Configure Dovecot Auth Mechanisms

```bash
nano /etc/dovecot/conf.d/10-auth.conf
```

Ensure these lines are set (uncomment if commented):

```conf
disable_plaintext_auth = yes
auth_mechanisms = plain login
```

Save and exit.

### Step 4.4: Create Mail User

We need a system user for SMTP authentication:

```bash
# Create mail user (no login shell for security)
useradd -r -s /bin/false mailuser

# Set password for SMTP authentication
passwd mailuser
# Enter a strong password (you'll use this in OpenMailer configuration)
```

**Save this password** - you'll need it later!

### Step 4.5: Restart Dovecot

```bash
systemctl restart dovecot
systemctl enable dovecot
systemctl status dovecot
```

Should show "active (running)".

---

## 🚀 Part 5: Install Let's Encrypt Certificate

### Step 5.1: Install Certbot

```bash
apt install -y certbot
```

### Step 5.2: Stop Postfix Temporarily

```bash
systemctl stop postfix
```

### Step 5.3: Get Certificate

```bash
# Replace mail.yourdomain.com with your actual mail subdomain
certbot certonly --standalone -d mail.yourdomain.com

# You'll be asked:
# 1. Enter email address (for renewal notifications)
# 2. Agree to Terms of Service: Yes
# 3. Share email with EFF: Your choice
```

**Expected output:**
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/mail.yourdomain.com/fullchain.pem
Key is saved at:         /etc/letsencrypt/live/mail.yourdomain.com/privkey.pem
```

### Step 5.4: Set Permissions

```bash
# Allow Postfix to read certificates
chmod 755 /etc/letsencrypt/live
chmod 755 /etc/letsencrypt/archive
```

### Step 5.5: Auto-Renewal

```bash
# Test renewal
certbot renew --dry-run

# Enable auto-renewal timer
systemctl enable certbot.timer
systemctl start certbot.timer

# Verify timer is active
systemctl list-timers
```

---

## 🚀 Part 6: Install and Configure OpenDKIM

OpenDKIM signs outgoing emails to prove they came from your server.

### Step 6.1: Install OpenDKIM

```bash
apt install -y opendkim opendkim-tools
```

### Step 6.2: Create Directories and Keys

```bash
# Create directory structure
mkdir -p /etc/opendkim/keys/yourdomain.com
cd /etc/opendkim/keys/yourdomain.com

# Generate DKIM keys
opendkim-genkey -b 2048 -d yourdomain.com -D /etc/opendkim/keys/yourdomain.com -s default -v

# Set ownership
chown -R opendkim:opendkim /etc/opendkim/keys/
chmod 600 /etc/opendkim/keys/yourdomain.com/default.private
```

### Step 6.3: Configure OpenDKIM

```bash
nano /etc/opendkim.conf
```

Replace entire contents with:

```conf
# Log to syslog
Syslog                  yes
SyslogSuccess           yes
LogWhy                  yes

# Required to use local socket with MTAs that access the socket as a non-
# privileged user (e.g. Postfix)
UMask                   002

# Commonly-used options
Canonicalization        relaxed/simple
Mode                    sv
SubDomains              no

# Socket for Postfix
Socket                  inet:8891@localhost

# Map domains to keys
KeyTable                refile:/etc/opendkim/key.table
SigningTable            refile:/etc/opendkim/signing.table

# Hosts to ignore
ExternalIgnoreList      /etc/opendkim/trusted.hosts
InternalHosts           /etc/opendkim/trusted.hosts
```

Save and exit.

### Step 6.4: Create Key Table

```bash
nano /etc/opendkim/key.table
```

Add (replace yourdomain.com):

```
default._domainkey.yourdomain.com yourdomain.com:default:/etc/opendkim/keys/yourdomain.com/default.private
```

Save and exit.

### Step 6.5: Create Signing Table

```bash
nano /etc/opendkim/signing.table
```

Add:

```
*@yourdomain.com default._domainkey.yourdomain.com
```

Save and exit.

### Step 6.6: Create Trusted Hosts

```bash
nano /etc/opendkim/trusted.hosts
```

Add:

```
127.0.0.1
localhost
yourdomain.com
mail.yourdomain.com
```

Save and exit.

### Step 6.7: Start OpenDKIM

```bash
systemctl restart opendkim
systemctl enable opendkim
systemctl status opendkim
```

Should show "active (running)".

### Step 6.8: Get DKIM Public Key

```bash
cat /etc/opendkim/keys/yourdomain.com/default.txt
```

You'll see something like:

```
default._domainkey      IN      TXT     ( "v=DKIM1; h=sha256; k=rsa; "
          "p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..." )  ; ----- DKIM key default for yourdomain.com
```

**Copy the part inside quotes** (starting with v=DKIM1...) - you'll need this for DNS!

---

## 🚀 Part 7: Start Postfix

Now that everything is configured:

```bash
systemctl restart postfix
systemctl enable postfix
systemctl status postfix
```

Should show "active (running)".

**Check for errors:**
```bash
tail -f /var/log/mail.log
```

Should show Postfix starting up. Press Ctrl+C to exit.

---

## 🚀 Part 8: DNS Configuration

This is **the most important step** for email deliverability!

### Step 8.1: Add A Record

In Cloudflare DNS:

1. **Type:** `A`
2. **Name:** `mail`
3. **IPv4 address:** `123.456.789.012` (your server IP)
4. **Proxy status:** Gray (DNS only) - **CRITICAL!**
5. **TTL:** Auto
6. Click **"Save"**

### Step 8.2: Add MX Record

1. **Type:** `MX`
2. **Name:** `@` (or leave blank for root domain)
3. **Mail server:** `mail.yourdomain.com`
4. **Priority:** `10`
5. **TTL:** Auto
6. Click **"Save"**

### Step 8.3: Add SPF Record

1. **Type:** `TXT`
2. **Name:** `@` (or leave blank)
3. **Content:** `v=spf1 mx a ip4:123.456.789.012 ~all`
   - Replace IP with your server IP
4. **TTL:** Auto
5. Click **"Save"**

### Step 8.4: Add DKIM Record

Remember the public key from Step 6.8? Use it here:

1. **Type:** `TXT`
2. **Name:** `default._domainkey`
3. **Content:** `v=DKIM1; h=sha256; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...`
   - Use your actual public key
   - Remove quotes and line breaks - make it one continuous string
4. **TTL:** Auto
5. Click **"Save"**

### Step 8.5: Add DMARC Record

1. **Type:** `TXT`
2. **Name:** `_dmarc`
3. **Content:** `v=DMARC1; p=quarantine; rua=mailto:postmaster@yourdomain.com; pct=100; adkim=s; aspf=s`
4. **TTL:** Auto
5. Click **"Save"**

### Step 8.6: Verify DNS Records

Wait 5-10 minutes for propagation, then verify:

```bash
# Check A record
nslookup mail.yourdomain.com

# Check MX record
nslookup -type=MX yourdomain.com

# Check SPF record
nslookup -type=TXT yourdomain.com

# Check DKIM record
nslookup -type=TXT default._domainkey.yourdomain.com

# Check DMARC record
nslookup -type=TXT _dmarc.yourdomain.com

# Check PTR (reverse DNS)
nslookup 123.456.789.012
```

All should return the values you configured.

---

## 🚀 Part 9: Testing

### Step 9.1: Test SMTP Authentication

From your local machine:

```bash
# Install openssl if not present
# Test connection to port 587
openssl s_client -starttls smtp -connect mail.yourdomain.com:587
```

Should show TLS connection success and certificate info. Type `QUIT` to exit.

### Step 9.2: Test Sending Email

```bash
# Install swaks (SMTP test tool)
apt install -y swaks

# Send test email
swaks --to your-email@gmail.com \
  --from test@yourdomain.com \
  --server mail.yourdomain.com:587 \
  --auth-user mailuser \
  --auth-password YOUR_MAILUSER_PASSWORD \
  --tls
```

**Check your Gmail inbox** (and spam folder) for the test email.

### Step 9.3: Test Email Authentication

Send email to: check-auth@verifier.port25.com

You'll receive an automated reply showing SPF, DKIM, and DMARC results.

### Step 9.4: Test Deliverability Score

1. Send email to: [https://www.mail-tester.com/](https://www.mail-tester.com/)
2. Copy the unique email address shown
3. Send test email to that address:

```bash
swaks --to test-abc123@srv1.mail-tester.com \
  --from noreply@yourdomain.com \
  --server mail.yourdomain.com:587 \
  --auth-user mailuser \
  --auth-password YOUR_PASSWORD \
  --tls \
  --header "Subject: Test Email" \
  --body "This is a test email from my custom SMTP server."
```

4. Go back to mail-tester.com and click "Then check your score"
5. **Target score: 8/10 or higher**

---

## 🚀 Part 10: Configure in OpenMailer

### Step 10.1: Login to OpenMailer

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "your-password"
  }'
```

### Step 10.2: Create SMTP Provider

```bash
curl -X POST http://localhost:8080/api/v1/providers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Custom SMTP Server",
    "type": "SMTP",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "Your App Name",
    "isDefault": true,
    "isActive": true,
    "configuration": {
      "host": "mail.yourdomain.com",
      "port": "587",
      "username": "mailuser",
      "password": "YOUR_MAILUSER_PASSWORD",
      "encryption": "TLS",
      "fromEmail": "noreply@yourdomain.com",
      "fromName": "Your App Name"
    }
  }'
```

### Step 10.3: Test via OpenMailer

```bash
curl -X POST http://localhost:8080/api/v1/providers/{PROVIDER_ID}/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "email": "your-email@gmail.com",
    "subject": "Test from Custom SMTP",
    "body": "This email was sent through my own SMTP server running Postfix!"
  }'
```

**Check your email!**

---

## ✅ Success Checklist

- [ ] VPS created and SSH accessible
- [ ] PTR (reverse DNS) record configured
- [ ] Hostname set to mail.yourdomain.com
- [ ] Firewall configured (UFW)
- [ ] Postfix installed and configured
- [ ] Dovecot installed and configured
- [ ] Mail user created with password
- [ ] Let's Encrypt certificate obtained
- [ ] OpenDKIM installed and configured
- [ ] DKIM keys generated
- [ ] DNS records added:
  - [ ] A record (mail.yourdomain.com)
  - [ ] MX record
  - [ ] SPF TXT record
  - [ ] DKIM TXT record
  - [ ] DMARC TXT record
- [ ] All DNS records verified with nslookup
- [ ] SMTP authentication tested with openssl
- [ ] Test email sent successfully
- [ ] Mail-tester score 8/10 or higher
- [ ] SMTP provider created in OpenMailer
- [ ] Production email sent via OpenMailer

---

## 🔧 Troubleshooting

### Issue 1: Connection Refused on Port 587

**Check if Postfix is running:**
```bash
systemctl status postfix
```

**Check if port is listening:**
```bash
netstat -tlnp | grep :587
```

**Check firewall:**
```bash
ufw status | grep 587
```

**Check logs:**
```bash
tail -50 /var/log/mail.log
```

---

### Issue 2: Authentication Failed

**Check Dovecot is running:**
```bash
systemctl status dovecot
```

**Verify auth socket exists:**
```bash
ls -la /var/spool/postfix/private/auth
```

**Test mail user password:**
```bash
# Install dovecot test tool
doveadm auth test mailuser YOUR_PASSWORD
```

---

### Issue 3: TLS/SSL Certificate Error

**Check certificate files exist:**
```bash
ls -la /etc/letsencrypt/live/mail.yourdomain.com/
```

**Test certificate:**
```bash
openssl s_client -starttls smtp -connect mail.yourdomain.com:587
```

**Renew certificate:**
```bash
certbot renew
systemctl restart postfix
```

---

### Issue 4: Emails Going to Spam

**Check authentication:**
```bash
# Send to check-auth@verifier.port25.com
# Should pass SPF, DKIM, and DMARC
```

**Check mail-tester.com score:**
- Should be 8/10 or higher
- Fix any issues it reports

**Build reputation:**
- Send to yourself first (Gmail, Outlook)
- Mark as "Not Spam"
- Send small volumes initially (10-20/day)
- Gradually increase over 2-4 weeks

---

### Issue 5: High Mail-Tester Score But Still Spam

**IP Reputation:**
- New IP addresses have no reputation
- Takes 2-4 weeks of consistent sending to build reputation
- Check IP reputation: [https://www.senderscore.org/](https://www.senderscore.org/)

**Content Issues:**
- Avoid spam trigger words (FREE, URGENT, CLICK HERE)
- Include plain text version
- Add unsubscribe link
- Don't use URL shorteners

**Volume:**
- Don't send 1000 emails on day 1
- Start with 10-20/day
- Increase by 20-30% every few days

---

### Issue 6: Cannot Send to Gmail

Gmail is the strictest. Common issues:

**PTR Record:**
```bash
# Must match exactly
nslookup YOUR_IP
# Should return: mail.yourdomain.com
```

**DMARC Policy:**
- Set to `p=quarantine` or `p=reject` (not `p=none`)
- Gmail trusts stricter policies more

**Postmaster Tools:**
1. Sign up: [https://postmaster.google.com/](https://postmaster.google.com/)
2. Add your domain
3. Monitor reputation and errors

---

## 🔒 Security Hardening

### Install Fail2ban

Prevents brute force attacks:

```bash
# Install
apt install -y fail2ban

# Create jail configuration
nano /etc/fail2ban/jail.local
```

Add:

```ini
[postfix-auth]
enabled = true
port = smtp,465,587
filter = postfix-auth
logpath = /var/log/mail.log
maxretry = 3
bantime = 600
```

Create filter:

```bash
nano /etc/fail2ban/filter.d/postfix-auth.conf
```

Add:

```ini
[Definition]
failregex = ^%(__prefix_line)swarning: [-._\w]+\[<HOST>\]: SASL (?:LOGIN|PLAIN|(?:CRAM|DIGEST)-MD5) authentication failed
ignoreregex =
```

Restart:

```bash
systemctl restart fail2ban
systemctl enable fail2ban
```

### Disable Root Login

```bash
# Create sudo user first
adduser yourusername
usermod -aG sudo yourusername

# Test SSH with new user before disabling root!
# In new terminal: ssh yourusername@YOUR_IP

# Then disable root login
nano /etc/ssh/sshd_config
# Set: PermitRootLogin no
systemctl restart sshd
```

### Enable Automatic Updates

```bash
apt install -y unattended-upgrades
dpkg-reconfigure -plow unattended-upgrades
# Select: Yes
```

---

## 📊 Monitoring

### Check Mail Queue

```bash
# View queue
mailq

# Clear queue
postsuper -d ALL
```

### View Logs

```bash
# Real-time log
tail -f /var/log/mail.log

# Last 50 lines
tail -50 /var/log/mail.log

# Search for errors
grep -i error /var/log/mail.log

# Search for specific email
grep "recipient@example.com" /var/log/mail.log
```

### Monitor Disk Space

```bash
df -h
```

If logs fill up disk, rotate them:

```bash
logrotate -f /etc/logrotate.d/rsyslog
```

---

## 💰 Cost Breakdown

### One-Time Costs:
- **Domain:** $10-15/year (you already have this)
- **Setup Time:** 8-12 hours × your hourly rate

### Monthly Costs:
- **VPS (2GB RAM):** $5-6/month
- **VPS (4GB RAM):** $12/month (recommended)

### Total First Month:
- $12 (VPS) + your time

### Total Ongoing:
- $12/month (VPS only)
- **No per-email costs!**

### Cost Comparison (100,000 emails/month):

| Provider | Monthly Cost |
|----------|-------------|
| Custom SMTP | $12 (fixed) |
| AWS SES | $10 |
| SendGrid | $89.95 |

---

## ⏭️ Next Steps

1. **Monitor for 1 week:**
   - Check `/var/log/mail.log` daily
   - Monitor mail queue: `mailq`
   - Track deliverability

2. **Build IP reputation:**
   - Send 10-20 emails/day for first week
   - Increase gradually
   - Mark test emails as "Not Spam"

3. **Set up monitoring:**
   - Install monitoring tool (Munin, Netdata)
   - Set up alerts for disk space
   - Monitor CPU/RAM usage

4. **Implement bounce handling:**
   - Parse bounce emails
   - Remove invalid addresses
   - See OpenMailer webhook documentation

5. **Consider backup provider:**
   - Keep AWS SES configured
   - Use as fallback if custom SMTP has issues

---

**Status:** ✅ Custom SMTP server complete!
**Difficulty:** ⭐⭐⭐⭐ Advanced
**Achievement Unlocked:** 🏆 You understand email infrastructure!
**Next:** Build reputation, monitor, and maintain
