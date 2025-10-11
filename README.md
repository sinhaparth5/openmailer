# OpenMailer 📧

Open-source email marketing SaaS built with Laravel & React. Send marketing emails from your own server using custom domains.

## 🎯 Description

OpenMailer is a self-hosted alternative to Mailchimp that uses PHPMailer to send emails directly from your server. Users can add custom domains, import contacts, create campaigns, and track email performance with detailed analytics.

## ✨ Features

- **Custom Domains** - Add & verify domains with DKIM/SPF/DMARC
- **Contact Management** - Import CSV, create lists, segment contacts
- **Campaign Builder** - Create, schedule & send bulk emails
- **Email Tracking** - Track opens, clicks, bounces, unsubscribes
- **Templates** - Reusable templates with personalization tags
- **Queue System** - Efficient bulk sending with Redis
- **Analytics** - Real-time stats & performance metrics

## 🚀 Quick Commands

```bash
# Install dependencies
composer install && npm install

# Setup database
php artisan migrate

# Start queue worker
php artisan queue:work redis --queue=emails

# Start scheduler (add to cron)
* * * * * php artisan schedule:run

# Start dev server
php artisan serve
```

## 📡 API Endpoints

### Campaigns
```
GET    /api/campaigns              # List all campaigns
POST   /api/campaigns              # Create campaign
GET    /api/campaigns/{id}         # Get campaign details
POST   /api/campaigns/{id}/send    # Send campaign
GET    /api/campaigns/{id}/stats   # Get campaign stats
```

### Contacts & Lists
```
GET    /api/contact-lists          # List all contact lists
POST   /api/contact-lists          # Create contact list
POST   /api/contact-lists/{id}/import  # Import CSV
GET    /api/contacts               # List all contacts
POST   /api/contacts               # Add contact
```

### Domains
```
GET    /api/domains                # List domains
POST   /api/domains                # Add domain
POST   /api/domains/{id}/verify    # Verify domain
GET    /api/domains/{id}/dns-instructions  # Get DNS setup
```

### Templates
```
GET    /api/templates              # List templates
POST   /api/templates              # Create template
PUT    /api/templates/{id}         # Update template
```

## 🔑 Authentication

All API endpoints require Bearer token:
```javascript
headers: {
  'Authorization': 'Bearer YOUR_TOKEN'
}
```

---

**License:** GPL-3 | **Tech Stack:** Laravel 11, React, PHPMailer, Redis
