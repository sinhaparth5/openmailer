# Email Provider Setup & Configuration Plan

**Created:** 2025-12-21
**Status:** Planning Phase
**Priority:** HIGH - Required for sending emails

---

## 📋 Overview

This plan outlines the setup and configuration of three email sending methods:
1. **AWS SES** (Simple Email Service) - Professional, scalable, cost-effective
2. **SendGrid** - Easy setup, generous free tier
3. **Custom SMTP Server** - Full control, learning experience

**Note:** The email infrastructure code already exists from Phase 1. This plan focuses on:
- Setup documentation
- DNS configuration guides
- Provider testing tools
- Configuration helpers

---

## 🎯 Goals

### Primary Goals:
1. ✅ Successfully send confirmation emails via at least one provider
2. ✅ Verify custom domain for email sending
3. ✅ Implement SPF, DKIM, and DMARC records
4. ✅ Test email deliverability
5. ✅ Create step-by-step setup guides

### Learning Goals:
1. 📚 Understand email authentication (SPF, DKIM, DMARC)
2. 📚 Learn AWS SES configuration and best practices
3. 📚 Understand SMTP protocol and mail server setup
4. 📚 Master DNS configuration for email

---

## 📊 Implementation Priority

### Phase A: Quick Win (SendGrid) - 1-2 hours
**Why First:** Fastest to implement, immediate results, free tier available

1. Create SendGrid account
2. Verify custom domain in SendGrid
3. Add DNS records in Cloudflare
4. Get API key
5. Configure in OpenMailer
6. Test confirmation emails

**Expected Time:** 1-2 hours
**Difficulty:** ⭐ Easy

---

### Phase B: Production Setup (AWS SES) - 2-4 hours
**Why Second:** Professional solution, very low cost, excellent deliverability

1. Create AWS account (or use existing)
2. Request production access (out of sandbox)
3. Verify domain in AWS SES
4. Configure DNS records (SPF, DKIM, DMARC)
5. Create IAM user with SES permissions
6. Get access keys
7. Configure in OpenMailer
8. Test and monitor

**Expected Time:** 2-4 hours
**Difficulty:** ⭐⭐ Moderate

---

### Phase C: Custom SMTP Server - 8-16 hours
**Why Last:** Most complex, learning-focused, requires server setup

#### Option C1: VPS with Postfix (Recommended for learning)
1. Set up VPS (DigitalOcean, AWS EC2, Hetzner)
2. Install Postfix mail server
3. Configure SMTP authentication
4. Set up DNS records (A, MX, SPF, DKIM, DMARC)
5. Configure TLS/SSL certificates
6. Implement DKIM signing
7. Test deliverability
8. Monitor and maintain

**Expected Time:** 8-12 hours
**Difficulty:** ⭐⭐⭐⭐ Advanced

#### Option C2: Docker Mail Server (Alternative)
1. Use docker-mailserver project
2. Configure with docker-compose
3. Set up DNS records
4. Configure DKIM, SPF, DMARC
5. Test and monitor

**Expected Time:** 4-8 hours
**Difficulty:** ⭐⭐⭐ Moderate-Advanced

---

## 📝 Detailed Implementation Plan

### 1️⃣ Phase A: SendGrid Setup (Quick Win)

#### Step 1: SendGrid Account Setup
- [ ] Sign up at sendgrid.com
- [ ] Verify email address
- [ ] Complete account setup

#### Step 2: Domain Verification
- [ ] Navigate to Settings > Sender Authentication
- [ ] Click "Authenticate Your Domain"
- [ ] Select your DNS provider (Cloudflare)
- [ ] Enter your domain name

#### Step 3: DNS Configuration in Cloudflare
SendGrid will provide records to add:
```dns
# CNAME Records (provided by SendGrid)
em1234.yourdomain.com    CNAME    u1234567.wl001.sendgrid.net
s1._domainkey           CNAME    s1.domainkey.u1234567.wl001.sendgrid.net
s2._domainkey           CNAME    s2.domainkey.u1234567.wl001.sendgrid.net
```

#### Step 4: Get API Key
- [ ] Navigate to Settings > API Keys
- [ ] Click "Create API Key"
- [ ] Select "Full Access" (or Mail Send only)
- [ ] Copy and save the API key securely

#### Step 5: Configure in OpenMailer
```bash
POST /api/v1/providers
{
  "name": "SendGrid Primary",
  "type": "SENDGRID",
  "fromEmail": "noreply@yourdomain.com",
  "fromName": "YourApp",
  "isDefault": true,
  "isActive": true,
  "configuration": {
    "apiKey": "SG.xxxxxxxxxxxxx",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "YourApp"
  }
}
```

#### Step 6: Test Email Sending
```bash
POST /api/v1/providers/{id}/test
{
  "email": "your-test-email@gmail.com",
  "subject": "Test Email",
  "body": "This is a test email from OpenMailer!"
}
```

---

### 2️⃣ Phase B: AWS SES Setup (Production)

#### Step 1: AWS Account Setup
- [ ] Create AWS account or log in
- [ ] Navigate to SES console
- [ ] Select your preferred region (e.g., us-east-1, eu-west-1)

#### Step 2: Request Production Access
- [ ] Go to "Account Dashboard" in SES
- [ ] Click "Request production access"
- [ ] Fill out the form:
  - Describe your use case
  - Explain how you handle bounces/complaints
  - Provide your website URL
- [ ] Wait for approval (usually 24 hours)

#### Step 3: Verify Domain
- [ ] Navigate to "Verified identities"
- [ ] Click "Create identity"
- [ ] Select "Domain"
- [ ] Enter your domain name
- [ ] Enable DKIM signing
- [ ] Copy the DNS records provided

#### Step 4: Configure DNS in Cloudflare
AWS will provide these records:
```dns
# DKIM Records (3 CNAME records)
abc123._domainkey.yourdomain.com    CNAME    abc123.dkim.amazonses.com
def456._domainkey.yourdomain.com    CNAME    def456.dkim.amazonses.com
ghi789._domainkey.yourdomain.com    CNAME    ghi789.dkim.amazonses.com

# MX Record (for receiving bounces - optional)
yourdomain.com    MX    10 feedback-smtp.us-east-1.amazonses.com

# TXT Record (SPF)
yourdomain.com    TXT    "v=spf1 include:amazonses.com ~all"

# TXT Record (DMARC)
_dmarc.yourdomain.com    TXT    "v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com"
```

#### Step 5: Create IAM User
- [ ] Navigate to IAM console
- [ ] Click "Users" > "Add user"
- [ ] Username: `openmailer-ses`
- [ ] Access type: "Programmatic access"
- [ ] Attach policy: `AmazonSESFullAccess` (or custom policy)
- [ ] Copy Access Key ID and Secret Access Key

#### Step 6: Configure in OpenMailer
```bash
POST /api/v1/providers
{
  "name": "AWS SES Production",
  "type": "AWS_SES",
  "fromEmail": "noreply@yourdomain.com",
  "fromName": "YourApp",
  "isDefault": true,
  "isActive": true,
  "configuration": {
    "region": "us-east-1",
    "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
    "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "YourApp"
  }
}
```

#### Step 7: Test and Monitor
- [ ] Send test email
- [ ] Check CloudWatch metrics
- [ ] Monitor bounce and complaint rates
- [ ] Set up SNS notifications (optional)

---

### 3️⃣ Phase C: Custom SMTP Server Setup

#### Prerequisites:
- VPS or dedicated server (2GB RAM minimum)
- Root/sudo access
- Basic Linux knowledge
- Your domain's DNS management access

#### Step 1: Server Setup (Using Ubuntu 22.04)
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Set hostname
sudo hostnamectl set-hostname mail.yourdomain.com

# Update /etc/hosts
echo "YOUR_SERVER_IP mail.yourdomain.com mail" | sudo tee -a /etc/hosts
```

#### Step 2: Install Postfix
```bash
# Install Postfix
sudo apt install postfix postfix-policyd-spf-python -y

# During installation:
# - Select "Internet Site"
# - System mail name: yourdomain.com
```

#### Step 3: Configure Postfix
Edit `/etc/postfix/main.cf`:
```conf
# Basic settings
myhostname = mail.yourdomain.com
mydomain = yourdomain.com
myorigin = $mydomain
mydestination = $myhostname, localhost.$mydomain, localhost
relayhost =
mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128

# SMTP settings
smtpd_banner = $myhostname ESMTP
smtpd_tls_cert_file=/etc/letsencrypt/live/mail.yourdomain.com/fullchain.pem
smtpd_tls_key_file=/etc/letsencrypt/live/mail.yourdomain.com/privkey.pem
smtpd_use_tls=yes
smtpd_tls_session_cache_database = btree:${data_directory}/smtpd_scache
smtp_tls_session_cache_database = btree:${data_directory}/smtp_scache

# SMTP Authentication
smtpd_sasl_type = dovecot
smtpd_sasl_path = private/auth
smtpd_sasl_auth_enable = yes
smtpd_recipient_restrictions = permit_sasl_authenticated,permit_mynetworks,reject_unauth_destination

# Anti-spam
smtpd_helo_required = yes
disable_vrfy_command = yes
```

#### Step 4: Install and Configure Dovecot (for SASL auth)
```bash
# Install Dovecot
sudo apt install dovecot-core dovecot-imapd dovecot-pop3d -y

# Configure Dovecot for Postfix SASL
# Edit /etc/dovecot/conf.d/10-master.conf
```

#### Step 5: Install Let's Encrypt Certificate
```bash
# Install Certbot
sudo apt install certbot -y

# Get certificate
sudo certbot certonly --standalone -d mail.yourdomain.com

# Auto-renewal
sudo systemctl enable certbot.timer
```

#### Step 6: Install OpenDKIM
```bash
# Install OpenDKIM
sudo apt install opendkim opendkim-tools -y

# Generate DKIM keys
sudo mkdir -p /etc/opendkim/keys/yourdomain.com
sudo opendkim-genkey -D /etc/opendkim/keys/yourdomain.com/ -d yourdomain.com -s default
sudo chown -R opendkim:opendkim /etc/opendkim/keys/

# Configure OpenDKIM
# Edit /etc/opendkim.conf
```

#### Step 7: DNS Configuration
```dns
# A Record
mail.yourdomain.com    A    YOUR_SERVER_IP

# MX Record
yourdomain.com    MX    10 mail.yourdomain.com

# SPF Record
yourdomain.com    TXT    "v=spf1 mx a ip4:YOUR_SERVER_IP ~all"

# DKIM Record (get from /etc/opendkim/keys/yourdomain.com/default.txt)
default._domainkey.yourdomain.com    TXT    "v=DKIM1; k=rsa; p=YOUR_PUBLIC_KEY"

# DMARC Record
_dmarc.yourdomain.com    TXT    "v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com"

# Reverse DNS (PTR) - Contact your VPS provider
YOUR_SERVER_IP    PTR    mail.yourdomain.com
```

#### Step 8: Configure in OpenMailer
```bash
POST /api/v1/providers
{
  "name": "Custom SMTP Server",
  "type": "SMTP",
  "fromEmail": "noreply@yourdomain.com",
  "fromName": "YourApp",
  "isDefault": true,
  "isActive": true,
  "configuration": {
    "host": "mail.yourdomain.com",
    "port": "587",
    "username": "noreply@yourdomain.com",
    "password": "your-smtp-password",
    "encryption": "TLS",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "YourApp"
  }
}
```

#### Step 9: Testing and Monitoring
```bash
# Test SMTP authentication
telnet mail.yourdomain.com 587

# Check mail logs
sudo tail -f /var/log/mail.log

# Test deliverability
mail-tester.com
```

---

## 🔧 Required OpenMailer Enhancements

### 1. Provider Test Endpoint Enhancement
**File:** `ProviderController.java`
**Current Status:** Already exists
**Enhancement Needed:** Better error messages

### 2. DNS Records Display
**New Endpoint:** `GET /api/v1/providers/{id}/dns-records`
**Purpose:** Show required DNS records for domain verification

### 3. Provider Setup Wizard
**New Endpoint:** `POST /api/v1/providers/setup-wizard`
**Purpose:** Step-by-step provider configuration

### 4. Email Deliverability Testing
**New Service:** `EmailDeliverabilityService`
**Purpose:** Test SPF, DKIM, DMARC configuration

---

## 📚 Documentation to Create

### 1. EMAIL_SETUP_SENDGRID.md
- Step-by-step SendGrid setup
- Screenshots
- Troubleshooting

### 2. EMAIL_SETUP_AWS_SES.md
- AWS account setup
- Domain verification
- Production access request
- IAM configuration

### 3. EMAIL_SETUP_SMTP.md
- VPS requirements
- Postfix installation
- OpenDKIM setup
- Troubleshooting common issues

### 4. DNS_CONFIGURATION.md
- SPF records explained
- DKIM records explained
- DMARC policy guide
- Reverse DNS (PTR) setup

---

## ✅ Success Criteria

### Phase A Success (SendGrid):
- [ ] Domain verified in SendGrid
- [ ] DNS records propagated
- [ ] API key working
- [ ] Test email sent successfully
- [ ] Confirmation email received in inbox (not spam)

### Phase B Success (AWS SES):
- [ ] Domain verified in AWS SES
- [ ] Production access granted
- [ ] DKIM signing enabled
- [ ] Test email sent successfully
- [ ] Bounce/complaint handling configured

### Phase C Success (Custom SMTP):
- [ ] Mail server installed and running
- [ ] TLS/SSL configured
- [ ] SMTP authentication working
- [ ] SPF, DKIM, DMARC configured
- [ ] Email passes mail-tester.com (score > 8/10)
- [ ] No reverse DNS issues
- [ ] Deliverability to Gmail/Outlook tested

---

## 📊 Cost Analysis

### SendGrid:
- **Free Tier:** 100 emails/day forever
- **Paid:** $19.95/month for 50,000 emails
- **Best For:** Getting started, small-medium projects

### AWS SES:
- **Cost:** $0.10 per 1,000 emails
- **Free Tier:** 62,000 emails/month (if hosted on AWS EC2)
- **Best For:** High volume, production apps

### Custom SMTP:
- **VPS Cost:** $5-20/month (DigitalOcean, Hetzner, etc.)
- **Best For:** Learning, full control, no per-email costs

---

## 🎓 Learning Resources

### Email Authentication:
- SPF: https://www.cloudflare.com/learning/dns/dns-records/dns-spf-record/
- DKIM: https://www.cloudflare.com/learning/dns/dns-records/dns-dkim-record/
- DMARC: https://www.cloudflare.com/learning/dns/dns-records/dns-dmarc-record/

### Postfix Setup:
- Official Docs: http://www.postfix.org/documentation.html
- DigitalOcean Guide: https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-postfix-on-ubuntu-22-04

### AWS SES:
- Getting Started: https://docs.aws.amazon.com/ses/latest/dg/quick-start.html
- Best Practices: https://docs.aws.amazon.com/ses/latest/dg/best-practices.html

---

## 🚀 Next Steps

1. **Choose Your First Provider:** SendGrid (recommended for quick start)
2. **Set Up DNS Access:** Ensure you can add records in Cloudflare
3. **Create Provider Account:** Sign up for chosen service
4. **Follow Setup Guide:** Use the detailed steps above
5. **Test Thoroughly:** Send test emails before going live
6. **Monitor:** Check deliverability and bounce rates

---

**Status:** Ready to begin implementation
**Recommended Start:** Phase A (SendGrid) for immediate results
**Time Commitment:** 1-2 hours for SendGrid, then expand to others
