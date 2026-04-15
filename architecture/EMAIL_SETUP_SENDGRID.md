# SendGrid Email Provider Setup Guide

**Difficulty:** ⭐ Easy
**Estimated Time:** 1-2 hours
**Cost:** Free tier (100 emails/day forever)
**Best For:** Quick setup, getting started, small-medium projects

---

## 📋 Prerequisites

Before you begin, make sure you have:

- [ ] A custom domain (e.g., yourdomain.com)
- [ ] Access to Cloudflare DNS management for your domain
- [ ] A valid email address for SendGrid account verification
- [ ] OpenMailer backend running locally or deployed

---

## 🚀 Step-by-Step Setup

### Step 1: Create SendGrid Account

1. Go to [https://sendgrid.com/](https://sendgrid.com/)
2. Click **"Start for Free"** or **"Sign Up"**
3. Fill in your details:
   - Email address
   - Password
   - Company name (can be personal project name)
4. Click **"Create Account"**
5. Check your email and **verify your email address**
6. Complete the account setup wizard:
   - Select your role (Developer)
   - Select your programming language (Java)
   - Select your goal (Transactional emails)

**Expected Result:** You should see the SendGrid dashboard.

---

### Step 2: Domain Authentication (Most Important!)

Domain authentication proves to email providers (Gmail, Outlook, etc.) that you own the domain and are authorized to send emails from it.

#### 2.1: Start Domain Authentication

1. In SendGrid dashboard, navigate to **Settings** → **Sender Authentication**
2. Click **"Authenticate Your Domain"** button
3. Select **"I'm not sure"** when asked about link branding (unless you know you need it)

#### 2.2: Select DNS Host

1. When asked "Which DNS host do you use?", select **"Other Host (not listed)"**
   - Even though you use Cloudflare, selecting "Other Host" gives you manual DNS records
2. Click **"Next"**

#### 2.3: Enter Your Domain

1. Enter your domain name (e.g., `yourdomain.com`)
   - **Do NOT include** `www.` or `http://`
   - Just the bare domain: `yourdomain.com`
2. Click **"Next"**

#### 2.4: Copy DNS Records

SendGrid will now show you the DNS records you need to add. You'll see something like:

```
┌─────────────────────────────────────────────────────────────────┐
│ Add these CNAME records to your DNS:                           │
├─────────────────────────────────────────────────────────────────┤
│ Host: em1234.yourdomain.com                                     │
│ Type: CNAME                                                     │
│ Value: u1234567.wl001.sendgrid.net                             │
├─────────────────────────────────────────────────────────────────┤
│ Host: s1._domainkey.yourdomain.com                             │
│ Type: CNAME                                                     │
│ Value: s1.domainkey.u1234567.wl001.sendgrid.net               │
├─────────────────────────────────────────────────────────────────┤
│ Host: s2._domainkey.yourdomain.com                             │
│ Type: CNAME                                                     │
│ Value: s2.domainkey.u1234567.wl001.sendgrid.net               │
└─────────────────────────────────────────────────────────────────┘
```

**Important:** Keep this page open! You'll need these values for the next step.

---

### Step 3: Add DNS Records in Cloudflare

Now you need to add the DNS records SendGrid provided to your Cloudflare DNS settings.

#### 3.1: Open Cloudflare Dashboard

1. Go to [https://dash.cloudflare.com/](https://dash.cloudflare.com/)
2. Log in to your account
3. Select your domain from the list
4. Click on **"DNS"** in the left sidebar

#### 3.2: Add First CNAME Record (Mail Subdomain)

1. Click **"Add record"** button
2. Fill in the form:
   - **Type:** `CNAME`
   - **Name:** `em1234` (just the subdomain part from SendGrid's first record)
     - If SendGrid shows `em1234.yourdomain.com`, enter only `em1234`
   - **Target:** `u1234567.wl001.sendgrid.net` (copy from SendGrid)
   - **Proxy status:** Click the orange cloud to make it **gray (DNS only)**
     - ⚠️ **CRITICAL:** Must be gray/DNS only, not orange/proxied!
   - **TTL:** Auto
3. Click **"Save"**

#### 3.3: Add Second CNAME Record (DKIM Key 1)

1. Click **"Add record"** button
2. Fill in the form:
   - **Type:** `CNAME`
   - **Name:** `s1._domainkey`
   - **Target:** `s1.domainkey.u1234567.wl001.sendgrid.net` (copy from SendGrid)
   - **Proxy status:** Gray (DNS only) - **CRITICAL!**
   - **TTL:** Auto
3. Click **"Save"**

#### 3.4: Add Third CNAME Record (DKIM Key 2)

1. Click **"Add record"** button
2. Fill in the form:
   - **Type:** `CNAME`
   - **Name:** `s2._domainkey`
   - **Target:** `s2.domainkey.u1234567.wl001.sendgrid.net` (copy from SendGrid)
   - **Proxy status:** Gray (DNS only) - **CRITICAL!**
   - **TTL:** Auto
3. Click **"Save"**

**Expected Result:** You should see 3 new CNAME records in your Cloudflare DNS list, all with gray cloud icons.

---

### Step 4: Verify Domain in SendGrid

#### 4.1: Wait for DNS Propagation

DNS changes can take time to propagate:
- **Minimum:** 5-10 minutes
- **Average:** 15-30 minutes
- **Maximum:** 48 hours (rare)

You can check DNS propagation at:
- [https://www.whatsmydns.net/](https://www.whatsmydns.net/)
- Enter your CNAME record (e.g., `em1234.yourdomain.com`)
- Select "CNAME" as record type
- Check if multiple locations show the SendGrid value

#### 4.2: Verify in SendGrid

1. Go back to the SendGrid page where you saw the DNS records
2. Click **"Verify"** button at the bottom
3. SendGrid will check your DNS records

**Possible Outcomes:**

✅ **Success:** "Domain verified!" - Continue to Step 5

⚠️ **Pending:** "DNS records not found yet" - Wait 10-15 more minutes and try again

❌ **Error:** "Invalid DNS records" - See Troubleshooting section below

---

### Step 5: Create API Key

Now that your domain is verified, you need an API key to send emails programmatically.

#### 5.1: Navigate to API Keys

1. In SendGrid dashboard, go to **Settings** → **API Keys**
2. Click **"Create API Key"** button (top right)

#### 5.2: Configure API Key

1. **API Key Name:** Enter a descriptive name (e.g., `OpenMailer Production`)
2. **API Key Permissions:** Select one of:
   - **Full Access** (easiest, recommended for getting started)
   - **Restricted Access** (more secure, select only "Mail Send" permissions)
3. Click **"Create & View"**

#### 5.3: Copy API Key

⚠️ **IMPORTANT:** You will only see this API key ONCE!

1. SendGrid will show your API key (starts with `SG.`)
2. **Copy the entire key** to a secure location:
   - Looks like: `SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
3. Save it to a password manager or secure note
4. Click **"Done"**

**Security Note:** Never commit this API key to Git or share it publicly!

---

### Step 6: Configure SendGrid in OpenMailer

Now you'll add SendGrid as an email provider in your OpenMailer backend.

#### 6.1: Start Your OpenMailer Backend

Make sure your Spring Boot application is running:

```bash
cd /home/parth/Documents/openmailer
./mvnw spring-boot:run
```

#### 6.2: Login to Get JWT Token

First, login to get your authentication token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "your-password"
  }'
```

Copy the `token` from the response. You'll use it in the next request.

#### 6.3: Create SendGrid Provider

Create a new email provider in OpenMailer:

```bash
curl -X POST http://localhost:8080/api/v1/providers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -d '{
    "name": "SendGrid Primary",
    "type": "SENDGRID",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "Your App Name",
    "isDefault": true,
    "isActive": true,
    "configuration": {
      "apiKey": "SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      "fromEmail": "noreply@yourdomain.com",
      "fromName": "Your App Name"
    }
  }'
```

**Replace:**
- `YOUR_JWT_TOKEN_HERE` - with your actual JWT token
- `yourdomain.com` - with your actual domain
- `Your App Name` - with your application name
- `SG.xxx...` - with your actual SendGrid API key

**Expected Response:**

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "SendGrid Primary",
    "type": "SENDGRID",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "Your App Name",
    "isDefault": true,
    "isActive": true,
    "createdAt": "2025-12-25T10:30:00Z"
  },
  "message": null,
  "timestamp": "2025-12-25T10:30:00Z"
}
```

---

### Step 7: Test Email Sending

Time to send your first email!

#### 7.1: Send Test Email

```bash
curl -X POST http://localhost:8080/api/v1/providers/{PROVIDER_ID}/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -d '{
    "email": "your-personal-email@gmail.com",
    "subject": "Test Email from OpenMailer",
    "body": "This is a test email sent through SendGrid! If you receive this, your email provider is configured correctly."
  }'
```

**Replace:**
- `{PROVIDER_ID}` - with the provider ID from Step 6.3 response
- `YOUR_JWT_TOKEN_HERE` - with your JWT token
- `your-personal-email@gmail.com` - with your actual email address

#### 7.2: Check Your Email

1. Wait 10-30 seconds
2. Check your email inbox
3. **Check spam folder** if you don't see it in inbox (first emails often go to spam)

**Expected Result:**

✅ **Success:** You received the email!
- Subject: "Test Email from OpenMailer"
- From: "Your App Name <noreply@yourdomain.com>"

⚠️ **In Spam:** Email received but in spam folder
- This is normal for the first few emails
- Will improve after sending more emails with good engagement

❌ **Not Received:** See Troubleshooting section

---

### Step 8: Send Confirmation Email (Production Use)

Now you can use this provider to send actual confirmation emails when users subscribe.

When a user subscribes via the public API:

```bash
POST http://localhost:8080/api/v1/public/subscribe/{listId}
{
  "email": "user@example.com",
  "name": "John Doe",
  "sendConfirmation": true
}
```

OpenMailer will automatically use your SendGrid provider to send the confirmation email!

---

## ✅ Success Checklist

Verify you've completed everything:

- [ ] SendGrid account created and verified
- [ ] Domain authenticated in SendGrid (green checkmark)
- [ ] 3 CNAME records added to Cloudflare DNS (all gray/DNS only)
- [ ] DNS records verified in SendGrid
- [ ] API key created and saved securely
- [ ] SendGrid provider created in OpenMailer
- [ ] Test email sent successfully
- [ ] Test email received (check spam folder)
- [ ] Confirmation emails working when users subscribe

---

## 🔧 Troubleshooting

### Issue 1: Domain Verification Fails

**Symptom:** SendGrid says "DNS records not found" even after waiting

**Solutions:**

1. **Check DNS records in Cloudflare:**
   - Go to Cloudflare DNS page
   - Verify all 3 CNAME records exist
   - Verify they are **gray (DNS only)**, NOT orange (proxied)
   - Click the orange cloud icon to toggle to gray if needed

2. **Verify record values exactly match:**
   - Go back to SendGrid's DNS records page
   - Compare each value character-by-character
   - Even one wrong character will cause failure

3. **Use DNS checker:**
   ```bash
   # Check CNAME records
   nslookup -type=CNAME em1234.yourdomain.com
   nslookup -type=CNAME s1._domainkey.yourdomain.com
   nslookup -type=CNAME s2._domainkey.yourdomain.com
   ```

4. **Wait longer:**
   - DNS can take up to 48 hours to propagate globally
   - Try verification again in 1 hour, then 6 hours, then 24 hours

---

### Issue 2: API Key Not Working

**Symptom:** Error: "Invalid API key" or "Unauthorized"

**Solutions:**

1. **Check API key format:**
   - Must start with `SG.`
   - Should be very long (70+ characters)
   - No spaces or line breaks

2. **Create new API key:**
   - Old key might have been revoked
   - Go to Settings → API Keys
   - Create a new one with Full Access
   - Update in OpenMailer configuration

3. **Check API key permissions:**
   - If using Restricted Access, verify "Mail Send" is enabled
   - Go to Settings → API Keys
   - Click on your key name to see permissions

---

### Issue 3: Emails Not Received

**Symptom:** Test email sent successfully but not received

**Solutions:**

1. **Check spam/junk folder:**
   - First few emails from a new domain often go to spam
   - Mark as "Not Spam" to improve future deliverability

2. **Check SendGrid Activity Feed:**
   - Go to SendGrid → Activity
   - Search for recipient email
   - Check delivery status:
     - **Delivered:** Email was accepted by recipient's server
     - **Bounced:** Email address doesn't exist
     - **Dropped:** SendGrid blocked it (e.g., invalid recipient)

3. **Verify sender email address:**
   - Must use your authenticated domain
   - ✅ Correct: `noreply@yourdomain.com`
   - ❌ Wrong: `noreply@gmail.com` (not your domain)

4. **Check email content:**
   - Avoid spam trigger words (FREE, CLICK HERE, etc.)
   - Include unsubscribe link for bulk emails
   - Don't send only images, include text

---

### Issue 4: Orange Cloud (Proxy) Error

**Symptom:** Cloudflare shows orange cloud icon for CNAME records

**Solution:**

1. Click the orange cloud icon to toggle it to gray
2. DNS records for email must be **DNS only (gray)**
3. Cloudflare proxy (orange) breaks email authentication
4. Save changes and wait 5 minutes
5. Verify domain again in SendGrid

---

### Issue 5: Rate Limit Exceeded

**Symptom:** Error: "Rate limit exceeded"

**Solutions:**

1. **Check your SendGrid plan:**
   - Free tier: 100 emails/day
   - If you need more, upgrade to paid plan

2. **Check daily quota:**
   - Go to SendGrid dashboard
   - See emails sent today vs. limit

3. **Wait for reset:**
   - Quota resets at midnight UTC
   - Or upgrade to paid plan for higher limits

---

## 📊 SendGrid Dashboard Overview

After setup, monitor your email sending:

### Activity Feed
- **Location:** SendGrid → Activity
- **Shows:** All sent emails with status
- **Useful for:** Debugging delivery issues

### Statistics
- **Location:** SendGrid → Stats
- **Shows:** Delivery rate, open rate, bounce rate
- **Useful for:** Monitoring email health

### Suppressions
- **Location:** SendGrid → Suppressions
- **Shows:** Bounced emails, spam reports, unsubscribes
- **Useful for:** Cleaning your email list

---

## 🎯 Best Practices

### 1. From Email Address
- Use a real subdomain: `noreply@yourdomain.com`
- Avoid: `no-reply@`, `donotreply@` (some email providers block these)
- Consider: `hello@`, `team@`, `notifications@`

### 2. Email Content
- Always include plain text version (not just HTML)
- Include unsubscribe link for marketing emails
- Keep subject lines under 50 characters
- Avoid all-caps and excessive punctuation!!!

### 3. Sending Volume
- Start slow: 10-20 emails/day for first week
- Gradually increase volume
- Sudden spikes trigger spam filters

### 4. Monitor Metrics
- **Bounce rate:** Should be < 2%
  - High bounce rate = bad email list
- **Spam complaint rate:** Should be < 0.1%
  - High rate = SendGrid may suspend account
- **Open rate:** Varies by industry (15-25% is typical)

### 5. List Hygiene
- Remove bounced emails from your database
- Honor unsubscribe requests immediately
- Never buy email lists (against SendGrid ToS)

---

## 💰 SendGrid Pricing (as of 2025)

### Free Tier
- **100 emails/day** (3,000/month)
- **Forever free**
- All core features included
- **Best for:** Development, testing, small projects

### Essentials Plan
- **Starting at $19.95/month**
- **50,000 emails/month**
- Email validation
- **Best for:** Growing applications

### Pro Plan
- **Starting at $89.95/month**
- **100,000+ emails/month**
- Dedicated IP address
- Advanced analytics
- **Best for:** High-volume applications

### Comparison with AWS SES
| Feature | SendGrid Free | AWS SES |
|---------|---------------|---------|
| Monthly emails | 3,000 | 62,000 (with EC2) |
| Cost/1,000 emails | $0 (up to limit) | $0.10 |
| Setup difficulty | ⭐ Easy | ⭐⭐ Moderate |
| Dashboard | Excellent | Basic |
| Support | Community | Pay for support |

---

## 🔗 Useful Links

- **SendGrid Dashboard:** [https://app.sendgrid.com/](https://app.sendgrid.com/)
- **API Documentation:** [https://docs.sendgrid.com/api-reference](https://docs.sendgrid.com/api-reference)
- **Best Practices:** [https://docs.sendgrid.com/ui/sending-email/deliverability](https://docs.sendgrid.com/ui/sending-email/deliverability)
- **Activity Feed:** [https://app.sendgrid.com/email_activity](https://app.sendgrid.com/email_activity)
- **Cloudflare DNS:** [https://dash.cloudflare.com/](https://dash.cloudflare.com/)

---

## ⏭️ Next Steps

After successfully setting up SendGrid:

1. **Test thoroughly:**
   - Send test emails to Gmail, Outlook, Yahoo
   - Check spam folder placement
   - Verify links and formatting

2. **Monitor for 1 week:**
   - Check SendGrid Activity Feed daily
   - Watch bounce and spam complaint rates
   - Adjust content if issues arise

3. **Consider adding AWS SES:**
   - For production applications
   - Better deliverability for high volume
   - Much lower cost at scale
   - See `EMAIL_SETUP_AWS_SES.md`

4. **Optional: Set up custom SMTP:**
   - For learning experience
   - Full control over infrastructure
   - See `EMAIL_SETUP_SMTP.md`

---

**Status:** ✅ SendGrid setup complete!
**Time to complete:** 1-2 hours
**Next recommended:** Test with real user signups, then add AWS SES for production
