# OpenMailer Frontend Implementation Plan

**Stack**: Thymeleaf + Tailwind CSS + Alpine.js
**Status**: Not Started (0%)
**Priority**: HIGH - Required for fullstack application

---

## 🎯 Overview

Build a modern, responsive web interface for OpenMailer using:
- **Thymeleaf** - Server-side templating (already configured)
- **Tailwind CSS** - Utility-first CSS framework
- **Alpine.js** - Lightweight JavaScript framework for interactivity
- **Chart.js** - For analytics and campaign statistics

---

## 📁 Project Structure

```
src/main/resources/
├── static/
│   ├── css/
│   │   └── styles.css (Tailwind compiled output)
│   ├── js/
│   │   ├── alpine.min.js
│   │   ├── chart.min.js
│   │   └── app.js (custom JavaScript)
│   └── images/
│       └── logo.svg
└── templates/
    ├── layout/
    │   ├── base.html (main layout)
    │   ├── sidebar.html (navigation)
    │   └── header.html (top bar)
    ├── auth/
    │   ├── login.html
    │   ├── register.html
    │   ├── forgot-password.html
    │   ├── reset-password.html
    │   └── setup-2fa.html
    ├── dashboard/
    │   └── index.html
    ├── contacts/
    │   ├── list.html
    │   ├── create.html
    │   ├── edit.html
    │   ├── import.html
    │   └── view.html
    ├── lists/
    │   ├── index.html
    │   ├── create.html
    │   ├── edit.html
    │   └── view.html
    ├── segments/
    │   ├── index.html
    │   ├── create.html
    │   └── edit.html
    ├── templates/
    │   ├── index.html
    │   ├── create.html
    │   ├── edit.html
    │   └── preview.html
    ├── campaigns/
    │   ├── index.html
    │   ├── create.html
    │   ├── edit.html
    │   ├── view.html
    │   └── analytics.html
    ├── domains/
    │   ├── index.html
    │   ├── add.html
    │   └── verify.html
    ├── providers/
    │   ├── index.html
    │   └── configure.html
    └── settings/
        ├── profile.html
        ├── security.html
        └── preferences.html
```

---

## 🎨 Design System

### Color Palette
```css
Primary: #3B82F6 (Blue)
Secondary: #8B5CF6 (Purple)
Success: #10B981 (Green)
Warning: #F59E0B (Amber)
Danger: #EF4444 (Red)
Neutral: #6B7280 (Gray)
```

### Typography
- **Headings**: Inter (Google Fonts)
- **Body**: System fonts
- **Code**: Fira Code

### Components
- **Buttons**: Primary, Secondary, Outline, Danger
- **Forms**: Input, Select, Textarea, Checkbox, Radio
- **Cards**: Default, Stat, Feature
- **Tables**: Responsive, Sortable, Paginated
- **Modals**: Confirmation, Form, Info
- **Alerts**: Success, Warning, Error, Info
- **Badges**: Status, Count
- **Dropdowns**: Menu, Action

---

## 📋 Implementation Phases

### Phase 1: Setup & Configuration (1-2 hours)

#### 1.1 Install Tailwind CSS
```bash
# Using CDN (quick start)
# Or npm for production build
npm init -y
npm install -D tailwindcss
npx tailwindcss init
```

#### 1.2 Configure Tailwind
**tailwind.config.js**:
```javascript
module.exports = {
  content: ["./src/main/resources/templates/**/*.html"],
  theme: {
    extend: {
      colors: {
        primary: '#3B82F6',
        secondary: '#8B5CF6',
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
```

#### 1.3 Add Alpine.js and Chart.js
Download or use CDN:
- Alpine.js: https://alpinejs.dev/
- Chart.js: https://www.chartjs.org/

#### 1.4 Create Base Layout
File: `templates/layout/base.html`

---

### Phase 2: Authentication Pages (3-4 hours)

#### Pages to Create:
1. **Login Page** (`auth/login.html`)
   - Email/password form
   - Remember me checkbox
   - Forgot password link
   - 2FA code input (conditional)

2. **Register Page** (`auth/register.html`)
   - Full name, email, password, confirm password
   - Terms and conditions checkbox
   - Email verification notice

3. **2FA Setup Page** (`auth/setup-2fa.html`)
   - QR code display
   - Backup codes display
   - Verification form

4. **Forgot Password** (`auth/forgot-password.html`)
   - Email input
   - Instructions

5. **Reset Password** (`auth/reset-password.html`)
   - New password form
   - Strength indicator

---

### Phase 3: Dashboard (4-5 hours)

#### Main Dashboard (`dashboard/index.html`)

**Sections**:
1. **Statistics Cards**
   - Total Contacts
   - Active Campaigns
   - Email Sent (This Month)
   - Open Rate Average

2. **Recent Campaigns**
   - Table with campaign name, status, sent, opens, clicks
   - Actions: View, Edit, Delete

3. **Activity Timeline**
   - Recent actions
   - Campaign sends
   - Contact imports

4. **Quick Actions**
   - Create Campaign
   - Import Contacts
   - Create List
   - Send Test Email

5. **Charts**
   - Email sent over time (Line chart)
   - Campaign performance (Bar chart)
   - Open/Click rates (Pie chart)

---

### Phase 4: Contact Management (6-8 hours)

#### 4.1 Contact List Page (`contacts/list.html`)
- Search and filter contacts
- Bulk actions (delete, export, add to list)
- Pagination
- Status badges (subscribed, unsubscribed, bounced)
- Tags display

#### 4.2 Create Contact (`contacts/create.html`)
- Form with all contact fields
- Tag management
- List assignment
- Status selection

#### 4.3 Edit Contact (`contacts/edit.html`)
- Pre-filled form
- Activity history
- Campaign history
- Unsubscribe link

#### 4.4 Import Contacts (`contacts/import.html`)
- CSV upload
- Field mapping
- Preview imported data
- Progress bar

#### 4.5 View Contact (`contacts/view.html`)
- Contact details
- Campaign interactions
- Opens and clicks timeline
- Tags and lists

---

### Phase 5: List Management (4-5 hours)

#### 5.1 Lists Index (`lists/index.html`)
- Grid/List view of contact lists
- Contact count per list
- Created date
- Actions: View, Edit, Delete

#### 5.2 Create/Edit List (`lists/create.html`, `lists/edit.html`)
- Name and description
- Default fields
- Settings (double opt-in, etc.)

#### 5.3 View List (`lists/view.html`)
- Contact table
- List statistics
- Export contacts
- Bulk actions

---

### Phase 6: Segment Management (3-4 hours)

#### 6.1 Segments Index (`segments/index.html`)
- List of segments
- Dynamic vs Static indicator
- Contact count (cached)
- Last calculated timestamp

#### 6.2 Create/Edit Segment (`segments/create.html`, `segments/edit.html`)
- Name and description
- Condition builder (visual)
- Preview matching contacts
- Type selection (dynamic/static)

---

### Phase 7: Template Management (6-8 hours)

#### 7.1 Templates Index (`templates/index.html`)
- Template cards with preview
- Search and filter
- Clone template action

#### 7.2 Create/Edit Template (`templates/create.html`, `templates/edit.html`)
- **Rich Text Editor** (TinyMCE or Quill)
- Variable insertion dropdown
- HTML/Visual toggle
- Preview pane (live)
- Subject line
- Preview text

#### 7.3 Template Preview (`templates/preview.html`)
- Full preview with sample data
- Send test email
- Mobile/Desktop view toggle

---

### Phase 8: Campaign Management (8-10 hours)

#### 8.1 Campaigns Index (`campaigns/index.html`)
- Campaign cards/table
- Status filters (Draft, Scheduled, Sending, Sent)
- Quick stats (sent, opens, clicks)
- Actions: Edit, Clone, View Analytics

#### 8.2 Create Campaign (`campaigns/create.html`)
**Multi-step wizard**:
1. Campaign Details (name, subject, preview text)
2. Select Template
3. Select Recipients (lists, segments)
4. Schedule or Send Now
5. Review and Confirm

#### 8.3 Edit Campaign (`campaigns/edit.html`)
- Only for DRAFT campaigns
- Same as create, pre-filled

#### 8.4 View Campaign (`campaigns/view.html`)
- Campaign details
- Sending progress (if in progress)
- Quick stats
- Recent activity

#### 8.5 Campaign Analytics (`campaigns/analytics.html`)
- **Comprehensive Dashboard**:
  - Total sent, delivered, bounced
  - Opens over time (chart)
  - Clicks over time (chart)
  - Top clicked links
  - Geographic distribution (if tracked)
  - Device/Client breakdown
  - Unsubscribe count
  - Complaint count

---

### Phase 9: Domain Management (3-4 hours)

#### 9.1 Domains Index (`domains/index.html`)
- List of domains
- Verification status badges
- SPF, DKIM, DMARC indicators
- Actions: Verify, View DNS, Delete

#### 9.2 Add Domain (`domains/add.html`)
- Domain name input
- Instructions for DNS setup

#### 9.3 Verify Domain (`domains/verify.html`)
- DNS records to add (copy buttons)
- Verification button
- Status checks (SPF, DKIM, DMARC)
- Troubleshooting tips

---

### Phase 10: Email Provider Configuration (2-3 hours)

#### 10.1 Providers Index (`providers/index.html`)
- List of configured providers
- Type badges (SMTP, SendGrid, AWS SES)
- Default indicator
- Active/Inactive toggle
- Test button

#### 10.2 Configure Provider (`providers/configure.html`)
- Provider type selection
- Dynamic form based on type
- Test email sending
- Save configuration

---

### Phase 11: Settings & Profile (3-4 hours)

#### 11.1 Profile Settings (`settings/profile.html`)
- User details (name, email)
- Avatar upload
- Timezone
- Language preferences

#### 11.2 Security Settings (`settings/security.html`)
- Change password
- 2FA toggle
- Active sessions
- API keys

#### 11.3 Preferences (`settings/preferences.html`)
- Email notifications
- Default sender
- Campaign defaults
- UI preferences

---

## 🛠️ Controllers Needed

Create Thymeleaf controllers for each page:

```java
@Controller
public class DashboardController {
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Add stats to model
        return "dashboard/index";
    }
}

@Controller
public class ContactViewController {
    @GetMapping("/contacts")
    public String listContacts(Model model) {
        return "contacts/list";
    }

    @GetMapping("/contacts/create")
    public String createContact() {
        return "contacts/create";
    }

    // ... more endpoints
}

// Similar controllers for:
// - ListViewController
// - SegmentViewController
// - TemplateViewController
// - CampaignViewController
// - DomainViewController
// - ProviderViewController
// - SettingsViewController
```

---

## 🎨 Tailwind Components to Build

### 1. Navigation Sidebar
```html
<aside class="w-64 bg-gray-900 min-h-screen text-white">
  <nav class="mt-5">
    <a href="/dashboard" class="flex items-center px-6 py-3 hover:bg-gray-800">
      <svg>...</svg>
      <span>Dashboard</span>
    </a>
    <!-- More nav items -->
  </nav>
</aside>
```

### 2. Stats Card
```html
<div class="bg-white rounded-lg shadow p-6">
  <div class="flex items-center">
    <div class="flex-1">
      <p class="text-sm text-gray-600">Total Contacts</p>
      <p class="text-2xl font-bold">12,345</p>
    </div>
    <div class="text-blue-500">
      <svg>...</svg>
    </div>
  </div>
  <p class="text-sm text-green-600 mt-2">
    <span>↑ 12%</span> from last month
  </p>
</div>
```

### 3. Data Table
```html
<table class="min-w-full divide-y divide-gray-200">
  <thead class="bg-gray-50">
    <tr>
      <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
      <!-- More headers -->
    </tr>
  </thead>
  <tbody class="bg-white divide-y divide-gray-200">
    <tr th:each="contact : ${contacts}">
      <td class="px-6 py-4" th:text="${contact.name}"></td>
      <!-- More cells -->
    </tr>
  </tbody>
</table>
```

### 4. Modal
```html
<div x-data="{ open: false }">
  <button @click="open = true">Open Modal</button>

  <div x-show="open" class="fixed inset-0 z-50">
    <!-- Backdrop -->
    <div class="fixed inset-0 bg-black bg-opacity-50"></div>

    <!-- Modal -->
    <div class="relative bg-white rounded-lg p-6 max-w-md mx-auto mt-20">
      <h3 class="text-lg font-bold">Modal Title</h3>
      <p class="mt-2">Modal content</p>
      <button @click="open = false" class="mt-4 btn-primary">Close</button>
    </div>
  </div>
</div>
```

---

## 🚀 Quick Start Implementation Order

### Week 1: Foundation & Auth (8-10 hours)
1. ✅ Setup Tailwind CSS
2. ✅ Create base layout with sidebar
3. ✅ Build authentication pages
4. ✅ Create reusable components

### Week 2: Core Features (15-20 hours)
5. ✅ Dashboard with charts
6. ✅ Contact management (CRUD)
7. ✅ List management
8. ✅ Template editor

### Week 3: Campaigns & Advanced (15-20 hours)
9. ✅ Campaign creation wizard
10. ✅ Campaign analytics
11. ✅ Segment builder
12. ✅ Domain verification UI

### Week 4: Polish & Testing (10-15 hours)
13. ✅ Provider configuration
14. ✅ Settings pages
15. ✅ Responsive design
16. ✅ Browser testing
17. ✅ Performance optimization

---

## 📦 Dependencies to Add

```xml
<!-- Add to pom.xml if not already present -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>

<dependency>
    <groupId>nz.net.ultraq.thymeleaf</groupId>
    <artifactId>thymeleaf-layout-dialect</artifactId>
</dependency>
```

---

## 🎯 Features to Include

### Core Features
- ✅ Responsive design (mobile-first)
- ✅ Dark mode toggle
- ✅ Real-time search
- ✅ Infinite scroll for lists
- ✅ Drag-and-drop file upload
- ✅ Toast notifications
- ✅ Loading states
- ✅ Error handling
- ✅ Form validation (client-side)
- ✅ Keyboard shortcuts

### Advanced Features
- ✅ Rich text editor for templates
- ✅ Visual segment builder
- ✅ Campaign wizard with steps
- ✅ Live chart updates
- ✅ Export to CSV/PDF
- ✅ Bulk actions
- ✅ Undo/Redo for editors

---

## 🧪 Testing Strategy

1. **Browser Testing**
   - Chrome, Firefox, Safari, Edge
   - Mobile browsers (iOS Safari, Chrome Mobile)

2. **Responsive Testing**
   - Mobile (320px - 767px)
   - Tablet (768px - 1023px)
   - Desktop (1024px+)

3. **Accessibility**
   - Keyboard navigation
   - Screen reader compatibility
   - WCAG 2.1 compliance

---

## 📈 Estimated Total Time

- **Setup**: 2 hours
- **Authentication**: 4 hours
- **Dashboard**: 5 hours
- **Contacts**: 8 hours
- **Lists**: 5 hours
- **Segments**: 4 hours
- **Templates**: 8 hours
- **Campaigns**: 10 hours
- **Domains**: 4 hours
- **Providers**: 3 hours
- **Settings**: 4 hours
- **Polish & Testing**: 10 hours

**Total**: ~67 hours (approximately 2 weeks of full-time work)

---

## 🎨 Design Resources

- **UI Kits**: Tailwind UI, DaisyUI, Flowbite
- **Icons**: Heroicons, Font Awesome
- **Fonts**: Google Fonts (Inter, Roboto)
- **Color Palettes**: Tailwind default colors
- **Inspiration**: Mailchimp, SendGrid, Mailgun dashboards

---

## ✅ Next Steps

1. **Immediate**: Set up Tailwind CSS and create base layout
2. **Then**: Build authentication pages
3. **After**: Create dashboard with real data
4. **Finally**: Implement all CRUD interfaces

---

**Ready to start building?** 🚀

We can start with:
1. Setting up Tailwind CSS
2. Creating the base layout with sidebar navigation
3. Building the login page

What would you like to tackle first?
