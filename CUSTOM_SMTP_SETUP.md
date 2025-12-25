# Custom SMTP Server Setup for Transactional Emails

**Purpose**: Send transactional emails (user confirmations, password resets, notifications)
**Estimated Time**: 8-12 hours
**Difficulty**: ⭐⭐⭐⭐ Advanced
**Priority**: HIGH - Required for user registration flow

---

## 📋 What You're Building

A production-ready SMTP server that can:
- ✅ Send confirmation emails for new user registrations
- ✅ Send password reset emails
- ✅ Send system notifications
- ✅ Pass spam filters (Gmail, Outlook, etc.)
- ✅ Use your custom domain (e.g., noreply@yourdomain.com)
- ✅ Secure SMTP with TLS encryption
- ✅ Authenticate emails with SPF, DKIM, and DMARC

---

## 🛠️ Prerequisites

### 1. VPS (Virtual Private Server)
You need a server to host your mail server. **Choose one**:

| Provider | Cost | Recommended Plan | RAM | Notes |
|----------|------|------------------|-----|-------|
| **DigitalOcean** | $6/month | Basic Droplet | 1GB | Easy to use, good docs |
| **Hetzner** | €4.15/month | CX11 | 2GB | Best value, EU-based |
| **Vultr** | $6/month | Regular Performance | 1GB | Good IP reputation |
| **AWS EC2** | ~$10/month | t3.micro | 1GB | Expensive, but scalable |

**CRITICAL**: Some VPS providers (like AWS EC2, Google Cloud) block port 25 by default. You need to:
- Request port 25 unblocking from support, OR
- Use port 587 for sending (SMTP submission)

### 2. Domain Setup
- You have a domain on Cloudflare ✅
- You need to create a subdomain: `mail.yourdomain.com`
- You'll add DNS records (A, MX, SPF, DKIM, DMARC)

### 3. Technical Skills Needed
- Basic Linux command line knowledge
- SSH access comfort
- Text editor usage (nano/vim)
- Understanding of DNS records

---

## 📊 Architecture Overview

```
OpenMailer App (Port 8080)
         ↓
   (connects to)
         ↓
Postfix SMTP Server (Port 587) ← TLS encrypted
         ↓
   (sends email)
         ↓
Recipient (Gmail, Outlook, etc.)
         ↑
    (verifies)
         ↑
DNS Records (SPF, DKIM, DMARC)
```

---

## 🚀 Step-by-Step Implementation

### Phase 1: VPS Setup (30 minutes)

#### 1.1 Create VPS
1. Sign up for chosen provider
2. Create new server:
   - **OS**: Ubuntu 22.04 LTS (recommended)
   - **RAM**: Minimum 1GB (2GB preferred)
   - **Region**: Choose closest to your users
   - **SSH Key**: Add your public key for security

#### 1.2 Initial Server Access
```bash
# SSH into your server
ssh root@YOUR_SERVER_IP

# Update system packages
apt update && apt upgrade -y

# Set timezone
timedatectl set-timezone America/New_York  # Change to your timezone

# Create hostname
hostnamectl set-hostname mail.yourdomain.com

# Update /etc/hosts
echo "YOUR_SERVER_IP mail.yourdomain.com mail" >> /etc/hosts
```

#### 1.3 Create Non-Root User (Security Best Practice)
```bash
# Create user
adduser openmailer
usermod -aG sudo openmailer

# Switch to new user
su - openmailer
```

---

### Phase 2: DNS Configuration (30 minutes)

Go to your Cloudflare dashboard and add these records:

#### 2.1 A Record (Points subdomain to server)
```
Type: A
Name: mail
Content: YOUR_SERVER_IP
TTL: Auto
Proxy: DNS only (disable orange cloud)
```

#### 2.2 MX Record (Tells world where to send email)
```
Type: MX
Name: @
Content: mail.yourdomain.com
Priority: 10
TTL: Auto
```

#### 2.3 SPF Record (Authorizes your server to send email)
```
Type: TXT
Name: @
Content: v=spf1 mx a ip4:YOUR_SERVER_IP ~all
TTL: Auto
```

#### 2.4 DMARC Record (Email authentication policy)
```
Type: TXT
Name: _dmarc
Content: v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@yourdomain.com
TTL: Auto
```

**Note**: We'll add DKIM record later after generating keys.

#### 2.5 Reverse DNS (PTR Record) - CRITICAL!
This is set by your VPS provider, not Cloudflare:

1. Go to your VPS provider's control panel
2. Find "Networking" or "IP Management"
3. Set PTR record to: `mail.yourdomain.com`

**Without reverse DNS, your emails will likely go to spam!**

---

### Phase 3: Install Postfix (1 hour)

```bash
# Install Postfix and required packages
sudo apt install -y postfix postfix-policyd-spf-python mailutils

# During installation, select:
# 1. "Internet Site"
# 2. System mail name: yourdomain.com
```

#### 3.1 Configure Postfix Main Settings
Edit `/etc/postfix/main.cf`:

```bash
sudo nano /etc/postfix/main.cf
```

Replace/add these settings:

```conf
# Basic Settings
myhostname = mail.yourdomain.com
mydomain = yourdomain.com
myorigin = $mydomain
mydestination = $myhostname, localhost.$mydomain, localhost, $mydomain
relayhost =
mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128
mailbox_size_limit = 0
recipient_delimiter = +
inet_interfaces = all
inet_protocols = all

# SMTP Banner
smtpd_banner = $myhostname ESMTP

# TLS/SSL Settings (we'll add certificates later)
smtpd_tls_cert_file=/etc/letsencrypt/live/mail.yourdomain.com/fullchain.pem
smtpd_tls_key_file=/etc/letsencrypt/live/mail.yourdomain.com/privkey.pem
smtpd_use_tls=yes
smtpd_tls_security_level=may
smtp_tls_security_level=may
smtpd_tls_session_cache_database = btree:${data_directory}/smtpd_scache
smtp_tls_session_cache_database = btree:${data_directory}/smtp_scache

# SMTP Authentication (we'll configure with Dovecot)
smtpd_sasl_type = dovecot
smtpd_sasl_path = private/auth
smtpd_sasl_auth_enable = yes
smtpd_sasl_security_options = noanonymous
smtpd_sasl_local_domain = $myhostname
broken_sasl_auth_clients = yes

# Restrictions (Anti-spam)
smtpd_recipient_restrictions =
    permit_sasl_authenticated,
    permit_mynetworks,
    reject_unauth_destination,
    reject_invalid_hostname,
    reject_non_fqdn_hostname,
    reject_non_fqdn_sender,
    reject_non_fqdn_recipient,
    reject_unknown_sender_domain,
    reject_unknown_recipient_domain,
    reject_rbl_client zen.spamhaus.org,
    reject_rbl_client bl.spamcop.net

smtpd_helo_required = yes
disable_vrfy_command = yes

# Milter configuration (for DKIM)
milter_default_action = accept
milter_protocol = 6
smtpd_milters = inet:127.0.0.1:8891
non_smtpd_milters = $smtpd_milters
```

#### 3.2 Configure Master Process
Edit `/etc/postfix/master.cf`:

```bash
sudo nano /etc/postfix/master.cf
```

Find the `submission` section and uncomment/modify:

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
```

---

### Phase 4: Install SSL/TLS Certificates (30 minutes)

```bash
# Install Certbot
sudo apt install -y certbot

# Stop Postfix temporarily
sudo systemctl stop postfix

# Get certificate
sudo certbot certonly --standalone -d mail.yourdomain.com

# When prompted:
# - Enter your email address
# - Agree to terms
# - Choose whether to share email with EFF

# Start Postfix
sudo systemctl start postfix

# Set up auto-renewal
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

**Verify certificate**:
```bash
sudo ls -la /etc/letsencrypt/live/mail.yourdomain.com/
# You should see: fullchain.pem and privkey.pem
```

---

### Phase 5: Install Dovecot (SASL Authentication) (45 minutes)

```bash
# Install Dovecot
sudo apt install -y dovecot-core dovecot-imapd dovecot-pop3d
```

#### 5.1 Configure Dovecot for Postfix SASL
Edit `/etc/dovecot/conf.d/10-master.conf`:

```bash
sudo nano /etc/dovecot/conf.d/10-master.conf
```

Find the `service auth` section and modify:

```conf
service auth {
  unix_listener /var/spool/postfix/private/auth {
    mode = 0666
    user = postfix
    group = postfix
  }
}
```

#### 5.2 Configure Authentication
Edit `/etc/dovecot/conf.d/10-auth.conf`:

```bash
sudo nano /etc/dovecot/conf.d/10-auth.conf
```

Set:
```conf
disable_plaintext_auth = yes
auth_mechanisms = plain login
```

#### 5.3 Create Virtual Users File
Since you don't need actual mailboxes (only sending), create a simple user file:

```bash
# Create password file
sudo mkdir -p /etc/dovecot/users
sudo nano /etc/dovecot/users/passwd
```

Add a user for SMTP authentication:
```
noreply@yourdomain.com:{PLAIN}your-strong-password-here
```

Edit `/etc/dovecot/conf.d/auth-passwdfile.conf.ext`:
```conf
passdb {
  driver = passwd-file
  args = /etc/dovecot/users/passwd
}

userdb {
  driver = static
  args = uid=vmail gid=vmail home=/var/mail/vhosts/%d/%n
}
```

Create vmail user:
```bash
sudo groupadd -g 5000 vmail
sudo useradd -g vmail -u 5000 vmail -d /var/mail
```

#### 5.4 Restart Dovecot
```bash
sudo systemctl restart dovecot
sudo systemctl enable dovecot
```

---

### Phase 6: Install OpenDKIM (Email Signing) (45 minutes)

```bash
# Install OpenDKIM
sudo apt install -y opendkim opendkim-tools
```

#### 6.1 Generate DKIM Keys
```bash
# Create directory
sudo mkdir -p /etc/opendkim/keys/yourdomain.com

# Generate keys
sudo opendkim-genkey -D /etc/opendkim/keys/yourdomain.com/ -d yourdomain.com -s default

# Set permissions
sudo chown -R opendkim:opendkim /etc/opendkim/keys/
sudo chmod 600 /etc/opendkim/keys/yourdomain.com/default.private
```

#### 6.2 Configure OpenDKIM
Edit `/etc/opendkim.conf`:

```bash
sudo nano /etc/opendkim.conf
```

Add/modify:
```conf
Syslog                  yes
SyslogSuccess           yes
LogWhy                  yes

UMask                   002

Domain                  yourdomain.com
Selector                default
KeyFile                 /etc/opendkim/keys/yourdomain.com/default.private

Socket                  inet:8891@localhost

PidFile                 /var/run/opendkim/opendkim.pid

Canonicalization        relaxed/simple
Mode                    sv
SubDomains              no
AutoRestart             yes
AutoRestartRate         10/1M
Background              yes
DNSTimeout              5
SignatureAlgorithm      rsa-sha256

# Trusted hosts
ExternalIgnoreList      /etc/opendkim/TrustedHosts
InternalHosts           /etc/opendkim/TrustedHosts
```

#### 6.3 Create Trusted Hosts
```bash
sudo nano /etc/opendkim/TrustedHosts
```

Add:
```
127.0.0.1
localhost
*.yourdomain.com
```

#### 6.4 Restart OpenDKIM
```bash
sudo systemctl restart opendkim
sudo systemctl enable opendkim
```

#### 6.5 Get DKIM Public Key for DNS
```bash
sudo cat /etc/opendkim/keys/yourdomain.com/default.txt
```

You'll see something like:
```
default._domainkey      IN      TXT     ( "v=DKIM1; k=rsa; "
          "p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC..." )  ; ----- DKIM key default for yourdomain.com
```

#### 6.6 Add DKIM Record to Cloudflare
In Cloudflare DNS:
```
Type: TXT
Name: default._domainkey
Content: v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC...
TTL: Auto
```

**Note**: Remove line breaks and quotes - paste only the content part!

---

### Phase 7: Restart All Services (5 minutes)

```bash
# Restart everything
sudo systemctl restart postfix
sudo systemctl restart dovecot
sudo systemctl restart opendkim

# Check status
sudo systemctl status postfix
sudo systemctl status dovecot
sudo systemctl status opendkim

# Check for errors
sudo tail -f /var/log/mail.log
```

---

### Phase 8: Test SMTP Server (30 minutes)

#### 8.1 Test Local Mail Sending
```bash
echo "Test email body" | mail -s "Test Subject" your-email@gmail.com
```

Check `/var/log/mail.log` for sending status.

#### 8.2 Test SMTP Authentication
```bash
# Install telnet if not present
sudo apt install -y telnet

# Test connection
telnet mail.yourdomain.com 587

# You should see:
# 220 mail.yourdomain.com ESMTP

# Type:
EHLO test
# You should see STARTTLS and AUTH options

# Type:
QUIT
```

#### 8.3 Test from OpenMailer
Update your `.env` or application properties:

```properties
# SMTP Configuration
spring.mail.host=mail.yourdomain.com
spring.mail.port=587
spring.mail.username=noreply@yourdomain.com
spring.mail.password=your-strong-password-here
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

Configure SMTP provider in OpenMailer:
```bash
curl -X POST http://localhost:8080/api/v1/providers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Custom SMTP Server",
    "type": "SMTP",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "OpenMailer",
    "isDefault": true,
    "isActive": true,
    "configuration": {
      "host": "mail.yourdomain.com",
      "port": "587",
      "username": "noreply@yourdomain.com",
      "password": "your-strong-password-here",
      "encryption": "TLS",
      "fromEmail": "noreply@yourdomain.com",
      "fromName": "OpenMailer"
    }
  }'
```

---

### Phase 9: Deliverability Testing (30 minutes)

#### 9.1 Test with Mail-Tester
1. Go to https://www.mail-tester.com/
2. Copy the unique email address shown
3. Send a test email from OpenMailer to that address
4. Click "Then check your score"
5. **Target**: 8/10 or higher

#### 9.2 Common Issues and Fixes

**Score < 8/10**: Check these:
- ❌ Reverse DNS (PTR) not set → Contact VPS provider
- ❌ DKIM not signing → Check OpenDKIM logs
- ❌ SPF failing → Verify SPF record
- ❌ No DMARC → Add DMARC record

#### 9.3 Test Gmail Delivery
1. Send test email to Gmail address
2. Open email in Gmail
3. Click three dots → "Show original"
4. Check:
   - SPF: PASS
   - DKIM: PASS
   - DMARC: PASS

---

## 🔐 Security Hardening

### 1. Firewall Configuration
```bash
# Install UFW
sudo apt install -y ufw

# Allow SSH
sudo ufw allow 22/tcp

# Allow SMTP
sudo ufw allow 25/tcp
sudo ufw allow 587/tcp

# Enable firewall
sudo ufw enable
```

### 2. Fail2Ban (Brute Force Protection)
```bash
# Install Fail2Ban
sudo apt install -y fail2ban

# Create local config
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# Edit config
sudo nano /etc/fail2ban/jail.local
```

Add Postfix protection:
```conf
[postfix]
enabled = true
port = smtp,465,587
filter = postfix
logpath = /var/log/mail.log
maxretry = 5
bantime = 600
```

```bash
# Restart Fail2Ban
sudo systemctl restart fail2ban
sudo systemctl enable fail2ban
```

### 3. Regular Updates
```bash
# Create cron job for auto-updates
sudo nano /etc/cron.weekly/updates

# Add:
#!/bin/bash
apt update && apt upgrade -y
```

```bash
# Make executable
sudo chmod +x /etc/cron.weekly/updates
```

---

## 📊 Monitoring and Maintenance

### Check Mail Logs
```bash
# Real-time monitoring
sudo tail -f /var/log/mail.log

# Search for errors
sudo grep -i error /var/log/mail.log

# Check specific email
sudo grep "recipient@example.com" /var/log/mail.log
```

### Monitor Queue
```bash
# Check mail queue
mailq

# View specific message
postcat -q MESSAGE_ID

# Flush queue (retry sending)
postfix flush

# Delete message from queue
postsuper -d MESSAGE_ID
```

### Check Service Status
```bash
# Check all mail services
sudo systemctl status postfix dovecot opendkim

# Check resource usage
htop
```

---

## ✅ Success Checklist

- [ ] VPS created and accessible via SSH
- [ ] DNS records configured (A, MX, SPF, DMARC, DKIM)
- [ ] Reverse DNS (PTR) set to mail.yourdomain.com
- [ ] Postfix installed and configured
- [ ] SSL/TLS certificate installed
- [ ] Dovecot SASL authentication working
- [ ] OpenDKIM signing emails
- [ ] Test email sent successfully
- [ ] Mail-tester.com score > 8/10
- [ ] Gmail shows SPF/DKIM/DMARC PASS
- [ ] OpenMailer configured with SMTP provider
- [ ] User confirmation emails working
- [ ] Firewall configured
- [ ] Fail2Ban installed

---

## 🆘 Troubleshooting

### Email Goes to Spam
**Check**:
1. Reverse DNS set correctly
2. DKIM signature present
3. SPF record correct
4. IP not blacklisted: https://mxtoolbox.com/blacklists.aspx

### Cannot Connect to Port 587
**Check**:
1. Postfix running: `sudo systemctl status postfix`
2. Port open: `sudo netstat -tulpn | grep 587`
3. Firewall allows 587: `sudo ufw status`

### Authentication Fails
**Check**:
1. Dovecot running: `sudo systemctl status dovecot`
2. Socket exists: `ls -la /var/spool/postfix/private/auth`
3. Credentials correct in `/etc/dovecot/users/passwd`

### DKIM Not Signing
**Check**:
1. OpenDKIM running: `sudo systemctl status opendkim`
2. Socket accessible: `sudo netstat -tulpn | grep 8891`
3. Key permissions: `ls -la /etc/opendkim/keys/yourdomain.com/`
4. Mail log: `sudo grep -i dkim /var/log/mail.log`

---

## 📚 Additional Resources

- Postfix Documentation: http://www.postfix.org/documentation.html
- OpenDKIM Setup: http://www.opendkim.org/
- MXToolbox (DNS checker): https://mxtoolbox.com/
- Mail Tester: https://www.mail-tester.com/
- Blacklist Checker: https://mxtoolbox.com/blacklists.aspx

---

## 💰 Cost Breakdown

- **VPS**: $6/month (DigitalOcean) or €4.15/month (Hetzner)
- **Domain**: Already have ✅
- **SSL Certificate**: Free (Let's Encrypt) ✅
- **Total Monthly**: ~$6/month for unlimited transactional emails

---

## 🎯 Next Steps After Setup

Once your custom SMTP server is working:

1. ✅ **User Confirmation Emails**: Test registration flow
2. ✅ **Password Reset Emails**: Implement and test
3. ⏭️ **Phase 10**: Set up SendGrid/AWS SES for campaigns
4. ⏭️ **Phase 9**: Implement caching and performance optimizations
5. ⏭️ **Phase 11**: Testing and documentation

---

**Status**: Ready to implement
**Estimated Total Time**: 8-12 hours
**Difficulty**: Advanced (but well-documented)
