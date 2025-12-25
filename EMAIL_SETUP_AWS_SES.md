# AWS SES (Simple Email Service) Setup Guide

**Difficulty:** ⭐⭐ Moderate
**Estimated Time:** 2-4 hours (including production access approval wait time)
**Cost:** $0.10 per 1,000 emails (62,000 free/month with EC2)
**Best For:** Production applications, high volume, excellent deliverability

---

## 📋 Prerequisites

Before you begin, make sure you have:

- [ ] AWS account (or willingness to create one)
- [ ] Credit card for AWS account (required even for free tier)
- [ ] Custom domain (e.g., yourdomain.com)
- [ ] Access to Cloudflare DNS management
- [ ] Website or application description (for production access request)
- [ ] OpenMailer backend running

---

## 🎯 Overview

AWS SES has two modes:

1. **Sandbox Mode** (default):
   - Can only send to verified email addresses
   - Limited to 200 emails/day
   - Used for testing only

2. **Production Mode** (requires approval):
   - Can send to any email address
   - 50,000 emails/day initially (can request increase)
   - Required for real applications

**Timeline:**
- **Day 1:** Set up account, verify domain, request production access (1-2 hours)
- **Day 2-3:** Wait for AWS approval (usually 24 hours, up to 48 hours)
- **Day 3:** Complete setup and testing (30 minutes)

---

## 🚀 Step-by-Step Setup

### Part 1: AWS Account & Initial Setup

#### Step 1.1: Create AWS Account

If you don't have an AWS account:

1. Go to [https://aws.amazon.com/](https://aws.amazon.com/)
2. Click **"Create an AWS Account"**
3. Fill in your details:
   - Email address
   - Password
   - AWS account name (e.g., "Your Name - Personal")
4. Select **"Personal"** account type
5. Enter payment information (credit card required)
   - AWS free tier includes SES usage
   - You won't be charged unless you exceed free tier
6. Verify your phone number
7. Select **"Basic Support - Free"** plan
8. Complete account creation

**Expected Result:** You can log in to AWS Console

#### Step 1.2: Choose Your AWS Region

AWS SES is not available in all regions. Choose one close to you:

**Recommended Regions:**
- **US East (N. Virginia)** - `us-east-1` (most features, best for US/global)
- **US West (Oregon)** - `us-west-2` (good for US West Coast)
- **Europe (Ireland)** - `eu-west-1` (best for Europe)
- **Asia Pacific (Singapore)** - `ap-southeast-1` (best for Asia)

**How to select region:**
1. Log in to AWS Console: [https://console.aws.amazon.com/](https://console.aws.amazon.com/)
2. Top right corner: Click on the region dropdown
3. Select your preferred region (e.g., "US East (N. Virginia)")

⚠️ **Important:** Use the same region throughout this guide!

#### Step 1.3: Navigate to SES Console

1. In AWS Console, search bar at top: type **"SES"**
2. Click **"Amazon Simple Email Service"**
3. You should see the SES dashboard

**Expected Result:** You see "Amazon SES" dashboard with "Sandbox" warning

---

### Part 2: Domain Verification

#### Step 2.1: Create Identity

1. In SES Console, click **"Identities"** in left sidebar
2. Click **"Create identity"** button (orange)
3. Select **"Domain"** (not Email address)
4. Enter your domain:
   - **Domain:** `yourdomain.com` (without www or http://)
   - **Assign a default configuration set:** Leave blank for now
   - **Advanced DKIM settings:** Keep default (Easy DKIM enabled)
   - **DKIM signing key length:** 2048-bit (recommended)
   - **Publish DNS records to Route 53:** Leave unchecked (we're using Cloudflare)
5. Click **"Create identity"**

**Expected Result:** You see your domain listed with "Verification status: Pending"

#### Step 2.2: Get DNS Records

After creating the identity, AWS will show you DNS records to add:

1. Click on your domain name in the Identities list
2. Scroll down to **"DomainKeys Identified Mail (DKIM)"** section
3. You'll see 3 CNAME records that look like:

```
┌──────────────────────────────────────────────────────────────────┐
│ DKIM CNAME Record 1:                                             │
├──────────────────────────────────────────────────────────────────┤
│ Name: abc123def456._domainkey.yourdomain.com                     │
│ Type: CNAME                                                      │
│ Value: abc123def456.dkim.amazonses.com                          │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ DKIM CNAME Record 2:                                             │
├──────────────────────────────────────────────────────────────────┤
│ Name: ghi789jkl012._domainkey.yourdomain.com                     │
│ Type: CNAME                                                      │
│ Value: ghi789jkl012.dkim.amazonses.com                          │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ DKIM CNAME Record 3:                                             │
├──────────────────────────────────────────────────────────────────┤
│ Name: mno345pqr678._domainkey.yourdomain.com                     │
│ Type: CNAME                                                      │
│ Value: mno345pqr678.dkim.amazonses.com                          │
└──────────────────────────────────────────────────────────────────┘
```

4. Click **"Copy"** button next to each record (or write them down)
5. Keep this page open for reference

---

### Part 3: DNS Configuration in Cloudflare

#### Step 3.1: Add DKIM Records

1. Go to [https://dash.cloudflare.com/](https://dash.cloudflare.com/)
2. Select your domain
3. Click **"DNS"** in left sidebar

For each of the 3 DKIM records from AWS:

1. Click **"Add record"**
2. Fill in:
   - **Type:** `CNAME`
   - **Name:** Extract subdomain from AWS record
     - AWS shows: `abc123def456._domainkey.yourdomain.com`
     - Enter only: `abc123def456._domainkey`
   - **Target:** Paste the value from AWS (e.g., `abc123def456.dkim.amazonses.com`)
   - **Proxy status:** Gray (DNS only) - **CRITICAL!**
   - **TTL:** Auto
3. Click **"Save"**
4. Repeat for all 3 DKIM records

**Expected Result:** 3 new CNAME records in Cloudflare, all gray (DNS only)

#### Step 3.2: Add SPF Record (Recommended)

SPF tells receiving servers which servers can send email for your domain.

1. Click **"Add record"**
2. Fill in:
   - **Type:** `TXT`
   - **Name:** `@` (or leave blank - represents your root domain)
   - **Content:** `v=spf1 include:amazonses.com ~all`
   - **TTL:** Auto
3. Click **"Save"**

**Note:** If you already have an SPF record, edit it to include `include:amazonses.com`:
```
v=spf1 include:amazonses.com include:_spf.google.com ~all
```

#### Step 3.3: Add DMARC Record (Recommended)

DMARC tells receiving servers what to do with emails that fail authentication.

1. Click **"Add record"**
2. Fill in:
   - **Type:** `TXT`
   - **Name:** `_dmarc`
   - **Content:** `v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain.com`
   - **TTL:** Auto
3. Click **"Save"**

**What this means:**
- `p=quarantine`: Failed emails go to spam (use `p=none` for monitoring only)
- `rua=mailto:dmarc@yourdomain.com`: Send reports to this email (optional)

---

### Part 4: Verify Domain in AWS

#### Step 4.1: Wait for DNS Propagation

DNS changes take time:
- **Minimum:** 5-10 minutes
- **Average:** 15-30 minutes
- **Maximum:** 48 hours (rare)

You can check with:
```bash
# Check DKIM records
nslookup -type=CNAME abc123def456._domainkey.yourdomain.com
nslookup -type=TXT yourdomain.com
nslookup -type=TXT _dmarc.yourdomain.com
```

#### Step 4.2: Check Verification Status

1. Go back to SES Console → Identities
2. Click on your domain name
3. Look at **"Verification status"**:
   - **Pending:** DNS not propagated yet, wait longer
   - **Successful:** ✅ Domain verified!
   - **Failed:** Check DNS records (see Troubleshooting)

AWS automatically checks every few minutes. Refresh the page to see updated status.

**Expected Result:** After 10-30 minutes, status changes to "Successful"

---

### Part 5: Request Production Access

While in sandbox mode, you can only send to verified email addresses. For real use, you need production access.

#### Step 5.1: Navigate to Account Dashboard

1. In SES Console, click **"Account dashboard"** in left sidebar
2. You'll see a banner: **"Your account is in the sandbox"**
3. Click **"Request production access"** button

#### Step 5.2: Fill Out Request Form

AWS needs to know you're not a spammer. Be honest and detailed:

1. **Mail type:**
   - Select **"Transactional"** (for confirmation emails, notifications)
   - OR **"Promotional"** (for marketing emails)

2. **Website URL:**
   - Enter your website: `https://yourdomain.com`
   - Or GitHub repo if no website yet: `https://github.com/yourusername/openmailer`

3. **Use case description:** (Important! Be detailed)

   Example for OpenMailer:
   ```
   I'm building an email marketing platform called OpenMailer for managing
   subscriber lists and campaigns. SES will be used to send:

   1. Double opt-in confirmation emails when users subscribe
   2. Welcome emails for new subscribers
   3. Email campaigns to segmented subscriber lists
   4. Unsubscribe confirmation emails

   Expected volume: 500-1,000 emails/day initially, growing to 5,000/day.

   Bounce handling: Automatically remove hard bounces from list.
   Complaint handling: One-click unsubscribe in all emails, automatic removal.

   The application includes:
   - Email verification (double opt-in)
   - Unsubscribe link in every email
   - Bounce/complaint tracking via SNS notifications
   ```

4. **Describe how you will comply with AWS Service Terms:**

   Example:
   ```
   - Only send to users who explicitly opted in
   - Include unsubscribe link in every email
   - Honor unsubscribe requests immediately
   - Never buy or rent email lists
   - Monitor bounce and complaint rates daily
   - Remove bounced/complained addresses automatically
   - Use double opt-in to verify all subscribers
   ```

5. **Acknowledge:**
   - ✅ Check "I will only send to recipients who have specifically requested my mail"
   - ✅ Check "I have a process to handle bounces and complaints"

6. Click **"Submit request"**

#### Step 5.3: Wait for Approval

AWS typically responds within:
- **Average:** 24 hours
- **Business hours:** Sometimes within a few hours
- **Maximum:** 48 hours

You'll receive an email at your AWS account email address with:
- ✅ **Approved:** "Your Amazon SES sending limit increase request has been granted"
- ❌ **Denied:** Explanation and option to resubmit (rare if you followed guide)

**What to do while waiting:**
- Continue with Step 6 (IAM user creation)
- Test in sandbox mode with verified email addresses
- Build/improve your application

---

### Part 6: Create IAM User for API Access

Instead of using your root AWS credentials (dangerous!), create a dedicated IAM user.

#### Step 6.1: Navigate to IAM

1. In AWS Console search bar, type **"IAM"**
2. Click **"IAM"** (Identity and Access Management)

#### Step 6.2: Create New User

1. Click **"Users"** in left sidebar
2. Click **"Create user"** button
3. **User name:** `openmailer-ses`
4. **Provide user access to AWS Management Console:** Leave UNCHECKED
   - We only need programmatic access (API keys)
5. Click **"Next"**

#### Step 6.3: Set Permissions

1. Select **"Attach policies directly"**
2. In search box, type: `ses`
3. Find and check **"AmazonSESFullAccess"**
   - Allows full SES permissions
   - For more security, create a custom policy with only SendEmail permission
4. Click **"Next"**
5. Click **"Create user"**

#### Step 6.4: Create Access Keys

1. Click on the user name (**openmailer-ses**)
2. Click **"Security credentials"** tab
3. Scroll to **"Access keys"** section
4. Click **"Create access key"**
5. Select **"Application running outside AWS"**
6. Click **"Next"**
7. **Description tag:** `OpenMailer Production` (optional)
8. Click **"Create access key"**

#### Step 6.5: Save Credentials

⚠️ **CRITICAL:** You'll only see the secret access key ONCE!

1. You'll see:
   - **Access key ID:** `AKIAIOSFODNN7EXAMPLE` (20 characters)
   - **Secret access key:** `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` (40 characters)

2. **Save both values securely:**
   - Download .csv file (recommended)
   - OR copy to password manager
   - OR save to secure note

3. Click **"Done"**

**Security Warning:**
- Never commit these to Git
- Never share publicly
- Treat like passwords

---

### Part 7: Configure AWS SES in OpenMailer

#### Step 7.1: Login to OpenMailer

Get your JWT token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-email@example.com",
    "password": "your-password"
  }'
```

Copy the `token` from response.

#### Step 7.2: Create AWS SES Provider

```bash
curl -X POST http://localhost:8080/api/v1/providers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -d '{
    "name": "AWS SES Production",
    "type": "AWS_SES",
    "fromEmail": "noreply@yourdomain.com",
    "fromName": "Your App Name",
    "isDefault": true,
    "isActive": true,
    "configuration": {
      "region": "us-east-1",
      "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
      "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
      "fromEmail": "noreply@yourdomain.com",
      "fromName": "Your App Name"
    }
  }'
```

**Replace:**
- `YOUR_JWT_TOKEN_HERE` - your JWT token
- `us-east-1` - your chosen AWS region
- `AKIAIOSFODNN7EXAMPLE` - your access key ID
- `wJalrXUtnFEMI...` - your secret access key
- `yourdomain.com` - your verified domain
- `Your App Name` - your application name

**Expected Response:**

```json
{
  "success": true,
  "data": {
    "id": "650e8400-e29b-41d4-a716-446655440001",
    "name": "AWS SES Production",
    "type": "AWS_SES",
    "fromEmail": "noreply@yourdomain.com",
    "isDefault": true,
    "isActive": true
  }
}
```

---

### Part 8: Testing

#### Step 8.1: Test in Sandbox Mode (Before Approval)

If still in sandbox, you can only send to verified addresses:

1. **Verify your test email:**
   - SES Console → Identities
   - Create identity → Email address
   - Enter your personal email
   - Check email and click verification link

2. **Send test email:**
```bash
curl -X POST http://localhost:8080/api/v1/providers/{PROVIDER_ID}/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "email": "your-verified-email@example.com",
    "subject": "Test from AWS SES",
    "body": "Testing AWS SES integration with OpenMailer!"
  }'
```

#### Step 8.2: Test in Production Mode (After Approval)

Once approved, test with any email:

```bash
curl -X POST http://localhost:8080/api/v1/providers/{PROVIDER_ID}/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "email": "any-email@gmail.com",
    "subject": "Test from AWS SES Production",
    "body": "This email proves production access is working!"
  }'
```

**Expected Result:**
- Email received within 10-30 seconds
- Check inbox and spam folder
- Should appear in inbox (better deliverability than SendGrid)

---

### Part 9: Monitoring & Maintenance

#### Step 9.1: CloudWatch Metrics

AWS automatically tracks metrics:

1. **Navigate to SES Console → Account dashboard**
2. Scroll to **"Sending statistics"** section
3. View graphs for:
   - **Sends:** Total emails sent
   - **Bounces:** Invalid email addresses
   - **Complaints:** Marked as spam
   - **Reject:** Blocked by AWS (e.g., invalid format)

**Healthy metrics:**
- Bounce rate < 5%
- Complaint rate < 0.1%

#### Step 9.2: Set Up CloudWatch Alarms (Optional)

Get notified of issues:

1. **Navigate to CloudWatch** (search in AWS Console)
2. **Alarms** → **Create alarm**
3. **Select metric:** SES → Bounce or Complaint
4. **Condition:** Greater than threshold
5. **Action:** Send SNS notification to your email

#### Step 9.3: Configure Bounce/Complaint Notifications (Recommended)

1. **Create SNS Topic:**
   - Navigate to SNS (Simple Notification Service)
   - Create topic: `ses-bounces`
   - Create subscription: Email → your email

2. **Configure SES to use SNS:**
   - SES Console → Identities
   - Click your domain
   - Scroll to **"Notifications"** section
   - Click **"Edit"**
   - **Bounces:** Select `ses-bounces` topic
   - **Complaints:** Select `ses-bounces` topic
   - Save

Now you'll receive email notifications when bounces/complaints occur.

---

## ✅ Success Checklist

- [ ] AWS account created
- [ ] SES region selected
- [ ] Domain identity created in SES
- [ ] 3 DKIM CNAME records added to Cloudflare (gray/DNS only)
- [ ] SPF TXT record added to Cloudflare
- [ ] DMARC TXT record added to Cloudflare
- [ ] Domain verification successful in SES
- [ ] Production access requested
- [ ] Production access approved (email received)
- [ ] IAM user created (`openmailer-ses`)
- [ ] Access keys created and saved securely
- [ ] AWS SES provider configured in OpenMailer
- [ ] Test email sent successfully
- [ ] CloudWatch monitoring reviewed
- [ ] (Optional) SNS notifications configured

---

## 🔧 Troubleshooting

### Issue 1: Domain Verification Stuck on "Pending"

**Solutions:**

1. **Check DNS records in Cloudflare:**
   ```bash
   nslookup -type=CNAME abc123def456._domainkey.yourdomain.com
   ```
   - Should return the AWS value
   - If not found, DNS not propagated yet

2. **Verify records are gray (DNS only):**
   - Orange cloud (proxy) breaks DKIM
   - Click to toggle to gray

3. **Check for typos:**
   - Compare Cloudflare records character-by-character with AWS
   - One wrong character = verification fails

4. **Wait longer:**
   - Can take up to 24 hours for global propagation

---

### Issue 2: Production Access Denied

**Reasons:**

1. **Vague use case description:**
   - Solution: Provide detailed explanation (see Step 5.2 example)

2. **No website/proof of legitimacy:**
   - Solution: Provide GitHub repo, staging site, or demo

3. **Suspicious activity history:**
   - Solution: Contact AWS Support to discuss

**How to resubmit:**
1. Wait 24 hours
2. Account dashboard → Request production access (available again)
3. Provide more details this time

---

### Issue 3: "Email address not verified" Error

**In Sandbox Mode:**
- Can only send to verified addresses
- Solution: Verify recipient email in SES Console → Identities

**In Production Mode:**
- Should not happen
- Verify account is actually out of sandbox:
  - Account dashboard → Look for "Sandbox" warning
  - If present, production access not yet approved

---

### Issue 4: High Bounce Rate

**Causes:**
- Invalid email addresses in your list
- Typos in email addresses
- Inactive/deleted accounts

**Solutions:**
1. **Remove bounced emails:**
   - Monitor SES bounce notifications
   - Delete hard bounces from your database immediately

2. **Use email validation:**
   - AWS SES has built-in validation API
   - Validate emails before adding to list

3. **Clean your list:**
   - Remove emails that haven't engaged in 6+ months

---

### Issue 5: Emails Going to Spam

**Solutions:**

1. **Warm up your sending:**
   - Start with 50 emails/day
   - Gradually increase over 2 weeks

2. **Improve email content:**
   - Avoid spam trigger words
   - Include plain text version
   - Add unsubscribe link

3. **Get users to whitelist:**
   - Ask subscribers to add you to contacts
   - "Please add us to your address book"

4. **Check reputation:**
   - Use mail-tester.com
   - Should score 8/10 or higher

---

### Issue 6: "AccessDenied" Error

**Cause:** IAM user doesn't have permission

**Solution:**
1. IAM Console → Users → openmailer-ses
2. Permissions tab
3. Verify **AmazonSESFullAccess** is attached
4. If not, click "Add permissions" → Attach policy

---

## 💰 Cost Analysis

### SES Pricing (as of 2025)

**Sending from EC2:**
- First 62,000 emails/month: **FREE**
- After that: $0.10 per 1,000 emails

**Sending from outside EC2:**
- $0.10 per 1,000 emails (no free tier)

**Receiving:**
- First 1,000 emails/month: **FREE**
- After that: $0.10 per 1,000 emails

**Data Transfer:**
- First 1 GB/month: FREE (AWS Free Tier)
- After: $0.09/GB

### Cost Comparison

| Volume/Month | SendGrid | AWS SES (EC2) | AWS SES (non-EC2) |
|--------------|----------|---------------|-------------------|
| 3,000 | Free | Free | $0.30 |
| 10,000 | Free* | Free | $1.00 |
| 50,000 | $19.95 | Free | $5.00 |
| 100,000 | $89.95 | $3.80 | $10.00 |
| 1,000,000 | $899+ | $94.00 | $100.00 |

*SendGrid free tier is 100/day = ~3,000/month

**SES is 10-20x cheaper at scale!**

---

## 🎯 Best Practices

### 1. Bounce Handling
```java
// Remove hard bounces immediately
if (bounceType.equals("Permanent")) {
    contactService.deleteByEmail(email);
}
```

### 2. Complaint Handling
```java
// Unsubscribe complainers immediately
if (complaintFeedbackType.equals("abuse")) {
    subscriberService.unsubscribe(email);
}
```

### 3. Rate Limiting
- Start: 50,000 emails/day (production default)
- Can request increase to 1,000,000+/day
- Respect rate limits (14 emails/second initially)

### 4. Email Reputation
- Monitor bounce rate < 5%
- Monitor complaint rate < 0.1%
- Bad rates = AWS may pause your account

### 5. Authentication
- Always use DKIM (enabled by default)
- Always set SPF record
- Always set DMARC record
- This prevents spoofing and improves deliverability

---

## 🔗 Useful Links

- **SES Console:** [https://console.aws.amazon.com/ses/](https://console.aws.amazon.com/ses/)
- **SES Documentation:** [https://docs.aws.amazon.com/ses/](https://docs.aws.amazon.com/ses/)
- **IAM Console:** [https://console.aws.amazon.com/iam/](https://console.aws.amazon.com/iam/)
- **CloudWatch:** [https://console.aws.amazon.com/cloudwatch/](https://console.aws.amazon.com/cloudwatch/)
- **SNS Console:** [https://console.aws.amazon.com/sns/](https://console.aws.amazon.com/sns/)
- **SES Pricing:** [https://aws.amazon.com/ses/pricing/](https://aws.amazon.com/ses/pricing/)

---

## ⏭️ Next Steps

After AWS SES setup:

1. **Monitor for 1 week:**
   - Check CloudWatch metrics daily
   - Watch bounce/complaint rates
   - Verify emails reaching inbox

2. **Set up bounce handling:**
   - Create SNS topic for notifications
   - Automatically remove bounces from database

3. **Request sending limit increase (if needed):**
   - Account dashboard → Request sending limit increase
   - Explain growth plans

4. **Consider custom SMTP server:**
   - For learning experience
   - See `EMAIL_SETUP_SMTP.md`

5. **Implement email templates:**
   - Use SES email templates feature
   - Or build template system in OpenMailer

---

**Status:** ✅ AWS SES setup complete!
**Production-ready:** Yes (after approval)
**Recommended for:** All production applications
**Next:** Monitor metrics and optimize deliverability
