# OpenMailer

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Laravel Version](https://img.shields.io/badge/Laravel-12.x-red.svg)](https://laravel.com)
[![Livewire Version](https://img.shields.io/badge/Livewire-3.x-blue.svg)](https://livewire.laravel.com)
[![PHP Version](https://img.shields.io/badge/PHP-8.3+-purple.svg)](https://php.net)

**OpenMailer** is a powerful, open-source email marketing platform built with Laravel and Livewire. Create, manage, and send marketing campaigns with ease while maintaining full control over your data and infrastructure.

## ✨ Features

### 🚀 Core Functionality
- **Campaign Management** - Create, schedule, and manage email campaigns with an intuitive interface
- **Contact Management** - Import, organize, and segment your subscriber lists
- **Template System** - WYSIWYG email editor with pre-built and custom templates
- **Batch Sending** - Scalable email delivery with queue-based processing
- **Real-time Analytics** - Track opens, clicks, bounces, and engagement metrics

### 🔧 Technical Features
- **Custom Domain Support** - Send emails from your own domain with DNS verification
- **Multiple Email Providers** - AWS SES, SendGrid, Postmark, and SMTP support
- **Compliance Ready** - GDPR, CAN-SPAM, and unsubscribe management
- **API Integration** - RESTful API for external integrations
- **Real-time UI** - Livewire-powered interface with instant updates

### 📊 Analytics & Reporting
- **Campaign Performance** - Detailed metrics and comparative analytics
- **Contact Insights** - Subscriber behavior and engagement tracking
- **Export Capabilities** - Download reports and contact data
- **A/B Testing** - Test subject lines and content variations

## 🛠️ Tech Stack

- **Backend**: Laravel 12 (PHP 8.3+)
- **Frontend**: Livewire 3 + Tailwind CSS
- **Database**: MySQL/PostgreSQL/SQLite
- **Queue System**: Redis/Database queues
- **Email Delivery**: AWS SES, SendGrid, Postmark
- **File Storage**: Local/S3/Digital Ocean Spaces

## 📋 Requirements

- PHP 8.3 or higher
- Composer
- Node.js & NPM
- Database (MySQL 8.0+, PostgreSQL 13+, or SQLite)
- Redis (recommended for queues)
- Email delivery service (AWS SES, SendGrid, etc.)

## 🚀 Quick Start

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/openmailer.git
   cd openmailer
   ```

2. **Install dependencies**
   ```bash
   composer install
   npm install
   ```

3. **Environment setup**
   ```bash
   cp .env.example .env
   php artisan key:generate
   ```

4. **Configure your database**
   ```env
   DB_CONNECTION=mysql
   DB_HOST=127.0.0.1
   DB_PORT=3306
   DB_DATABASE=openmailer
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   ```

5. **Run migrations**
   ```bash
   php artisan migrate
   ```

6. **Build assets**
   ```bash
   npm run build
   ```

7. **Start the development server**
   ```bash
   php artisan serve
   ```

Visit `http://localhost:8000` to access OpenMailer.

### Email Provider Setup

Configure your preferred email delivery service in `.env`:

#### AWS SES
```env
MAIL_MAILER=ses
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_DEFAULT_REGION=us-east-1
```

#### SendGrid
```env
MAIL_MAILER=sendgrid
SENDGRID_API_KEY=your_api_key
```

#### SMTP
```env
MAIL_MAILER=smtp
MAIL_HOST=your-smtp-host.com
MAIL_PORT=587
MAIL_USERNAME=your_username
MAIL_PASSWORD=your_password
MAIL_ENCRYPTION=tls
```

### Queue Configuration

For production environments, configure Redis for better performance:

```env
QUEUE_CONNECTION=redis
REDIS_HOST=127.0.0.1
REDIS_PASSWORD=null
REDIS_PORT=6379
```

Start the queue worker:
```bash
php artisan queue:work
```

## 📖 Usage Guide

### Creating Your First Campaign

1. **Add Contacts**
   - Navigate to Contacts → Import
   - Upload a CSV file with email addresses
   - Map columns to contact fields
   - Assign contacts to lists

2. **Create a Campaign**
   - Go to Campaigns → Create New
   - Choose your template or create from scratch
   - Select your target audience (contact lists)
   - Set subject line and sender information

3. **Send or Schedule**
   - Preview your campaign
   - Send test emails
   - Schedule for later or send immediately

### Managing Domains

1. **Add Your Domain**
   - Go to Settings → Domains
   - Add your sending domain (e.g., mail.yourdomain.com)

2. **Configure DNS**
   - Add the provided SPF, DKIM, and DMARC records to your DNS
   - Wait for verification (usually 5-15 minutes)

3. **Start Sending**
   - Once verified, you can send campaigns from your custom domain

## 🔧 Configuration

### Custom Fields

Add custom fields for contacts in Settings → Custom Fields:
- Text fields for names, company, etc.
- Date fields for birthdays, anniversaries
- Boolean fields for preferences
- Select fields for categories

### Email Templates

Create reusable templates:
- Use the WYSIWYG editor
- Add merge tags like `{{first_name}}` and `{{email}}`
- Save as templates for future campaigns
- Export/import templates between instances

### Automation Rules

Set up automated campaigns:
- Welcome emails for new subscribers
- Birthday campaigns
- Re-engagement sequences
- Drip campaigns based on actions

## 🏗️ Development

### Project Structure

```
app/
├── Http/
│   ├── Controllers/     # Web and API controllers
│   └── Livewire/       # Livewire components
├── Models/             # Eloquent models
├── Services/           # Business logic services
├── Repositories/       # Data access layer
├── Jobs/              # Queue jobs for email sending
└── Events/            # Event classes for tracking
```

### Running Tests

```bash
# Run all tests
php artisan test

# Run specific test suite
php artisan test --testsuite=Feature

# Run with coverage
php artisan test --coverage
```

### Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

### Code Style

We follow PSR-12 coding standards. Run the code formatter:

```bash
./vendor/bin/pint
```

## 📊 Performance

### Recommended Server Specs

**Small Scale** (< 10K contacts)
- 1 vCPU, 2GB RAM
- 20GB SSD storage
- 1Gbps network

**Medium Scale** (10K - 100K contacts)
- 2 vCPU, 4GB RAM
- 50GB SSD storage
- Redis for queues

**Large Scale** (100K+ contacts)
- 4+ vCPU, 8GB+ RAM
- 100GB+ SSD storage
- Dedicated Redis server
- Load balancer for multiple app servers

### Optimization Tips

- Use Redis for sessions and queues
- Enable database query caching
- Configure CDN for static assets
- Use database read replicas for analytics
- Implement horizontal scaling with multiple queue workers

## 🔒 Security

### Best Practices

- Keep Laravel and dependencies updated
- Use environment variables for sensitive data
- Enable rate limiting on API endpoints
- Implement proper CORS policies
- Regular security audits and backups

### Compliance

OpenMailer includes features for:
- **GDPR Compliance**: Data export, deletion, and consent management
- **CAN-SPAM**: Automatic unsubscribe links and sender identification
- **Privacy**: IP address and user agent logging for compliance

## 📈 Scaling

### Horizontal Scaling

1. **Multiple App Servers**
   - Use a load balancer (nginx, HAProxy)
   - Share sessions via Redis
   - Use external file storage (S3)

2. **Database Scaling**
   - Read replicas for analytics queries
   - Separate analytics database
   - Connection pooling

3. **Queue Workers**
   - Multiple queue workers on different servers
   - Dedicated servers for email sending
   - Queue priority management

## 🐛 Troubleshooting

### Common Issues

**Email Delivery Problems**
```bash
# Check queue status
php artisan queue:monitor

# Verify email provider configuration
php artisan config:cache

# Test email sending
php artisan tinker
Mail::raw('Test email', function($m) { $m->to('test@example.com'); });
```

**DNS Verification Issues**
- Ensure DNS records are properly formatted
- Allow 15-30 minutes for DNS propagation
- Use online DNS lookup tools to verify records
- Check for conflicting SPF records

**Performance Issues**
```bash
# Clear all caches
php artisan optimize:clear

# Restart queue workers
php artisan queue:restart

# Check database performance
php artisan telescope:clear
```

## 📚 API Documentation

OpenMailer provides a RESTful API for integration with external systems. See our [API Documentation](docs/API.md) for detailed endpoints and examples.

### Quick API Example

```php
// Create a contact via API
POST /api/contacts
{
    "email": "user@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "list_ids": ["uuid-of-list"]
}
```

## 📄 License

OpenMailer is open-sourced software licensed under the [MIT license](LICENSE).

## 🤝 Support

- **Documentation**: [docs.openmailer.dev](https://docs.openmailer.dev)
- **Issues**: [GitHub Issues](https://github.com/yourusername/openmailer/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/openmailer/discussions)
- **Security**: Send security issues to security@openmailer.dev

## 🎯 Roadmap

### Version 2.0
- [ ] Visual email builder with drag-and-drop
- [ ] Advanced automation workflows
- [ ] SMS marketing integration
- [ ] Multi-language support
- [ ] Enhanced A/B testing

### Version 2.1
- [ ] Social media integration
- [ ] Landing page builder
- [ ] CRM integration
- [ ] Advanced reporting dashboard
- [ ] Mobile app

## 🙏 Acknowledgments

- Laravel team for the amazing framework
- Livewire team for reactive components
- Email delivery providers for reliable infrastructure
- Open source community for inspiration and contributions

---

**Made with ❤️ by the OpenMailer team**

[Website](https://openmailer.dev) • [Documentation](https://docs.openmailer.dev) • [Twitter](https://twitter.com/openmailer)