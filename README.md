# OpenMailer

OpenMailer is an open source bulk email system with templates, built with Spring Boot 4.0, PostgreSQL, and Tailwind CSS.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Setup](#project-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Build Commands](#build-commands)
- [Database Commands](#database-commands)
- [Liquibase Commands](#liquibase-commands)
- [Tailwind CSS Commands](#tailwind-css-commands)
- [Git Commands](#git-commands)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)

---

## Prerequisites

- **Java 25** - [Download](https://jdk.java.net/25/)
- **Maven 3.9+** - (Included via Maven Wrapper)
- **Node.js 18+** - For Tailwind CSS compilation
- **PostgreSQL 14+** - Database server

---

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd openmailer
```

### 2. Install Node Dependencies

```bash
npm install
```

### 3. Configure Database

Copy the Liquibase properties template and configure your database:

```bash
cp src/main/resources/liquibase.properties.template src/main/resources/liquibase.properties
```

Edit `src/main/resources/application-dev.properties` and add your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/openmailer
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Build Tailwind CSS

```bash
npm run build:css
```

### 5. Build the Application

```bash
./mvnw clean package
```

---

## Configuration

### Environment Profiles

The application supports multiple profiles:

- **dev** - Development profile (default)
- **prod** - Production profile

Switch profiles by editing `application.properties`:

```properties
spring.profiles.active=dev
```

Or run with a specific profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Configuration Files

- `application.properties` - Main configuration
- `application-dev.properties` - Development settings
- `application-prod.properties` - Production settings
- `liquibase.properties` - Liquibase Maven plugin settings

---

## Running the Application

### Development Mode

```bash
./mvnw spring-boot:run
```

The application will start at: `http://localhost:8080`

### Production Mode

```bash
java -jar target/openmailer-0.0.1-SNAPSHOT.jar
```

### With Tailwind CSS Watch Mode (Recommended for Development)

Open two terminals:

**Terminal 1 - Application:**
```bash
./mvnw spring-boot:run
```

**Terminal 2 - Tailwind CSS:**
```bash
npm run watch:css
```

This auto-rebuilds CSS when you modify HTML templates.

---

## Build Commands

### Clean Build

```bash
./mvnw clean
```

### Compile

```bash
./mvnw compile
```

### Package (Skip Tests)

```bash
./mvnw clean package -DskipTests
```

### Package (With Tests)

```bash
./mvnw clean package
```

### Run Tests

```bash
./mvnw test
```

### Install to Local Maven Repository

```bash
./mvnw clean install
```

### Build for Production

```bash
./mvnw clean package -Pprod
npm run build:css:prod
```

---

## Database Commands

### Create Database (PostgreSQL)

```bash
createdb openmailer
```

Or using psql:

```sql
CREATE DATABASE openmailer;
```

### Connect to Database

```bash
psql -d openmailer -U your_username
```

### View Tables

```sql
\dt
```

### Drop Database

```bash
dropdb openmailer
```

---

## Liquibase Commands

### Generate Complete Changelog from Database

Creates XML from all existing tables:

```bash
./mvnw liquibase:generateChangeLog
```

Output: `src/main/resources/db/changelog/db.changelog-generated.xml`

### Generate Diff Between Database and Current State

After updating JPA entities, generate a changelog with differences:

```bash
./mvnw liquibase:diff
```

Output: `src/main/resources/db/changelog/changes/diff-{timestamp}.xml`

### Update Database

Apply all pending changelogs:

```bash
./mvnw liquibase:update
```

### View Database Status

See which changesets have been applied:

```bash
./mvnw liquibase:status
```

### Rollback Last Changeset

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Rollback to Date

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackDate=2025-01-01
```

### Validate Changelog

```bash
./mvnw liquibase:validate
```

### Clear Checksums

```bash
./mvnw liquibase:clearCheckSums
```

### Tag Database

```bash
./mvnw liquibase:tag -Dliquibase.tag=v1.0
```

### Rollback to Tag

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.0
```

---

## Tailwind CSS Commands

### Build CSS (One Time)

```bash
npm run build:css
```

### Watch CSS (Auto-rebuild on Changes)

```bash
npm run watch:css
```

Use this during development - it watches for HTML changes and rebuilds CSS automatically.

### Build CSS for Production (Minified)

```bash
npm run build:css:prod
```

---

## Git Commands

### Initialize Repository

```bash
git init
git add .
git commit -m "Initial commit"
```

### Create Commit

```bash
git add .
git commit -m "Your commit message"
```

### Push to Remote

```bash
git push origin master
```

### Create New Branch

```bash
git checkout -b feature/your-feature-name
```

### View Status

```bash
git status
```

### View Commit History

```bash
git log --oneline
```

---

## Project Structure

```
openmailer/
├── src/
│   ├── main/
│   │   ├── java/com/openmailer/openmailer/
│   │   │   ├── config/          # Configuration classes
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/      # REST and MVC controllers
│   │   │   │   └── HomeController.java
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── model/           # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── EmailTemplate.java
│   │   │   │   └── EmailCampaign.java
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── OpenmailerApplication.java
│   │   └── resources/
│   │       ├── db/changelog/
│   │       │   ├── changes/     # Individual migration files
│   │       │   │   ├── 001-create-users-table.xml
│   │       │   │   ├── 002-create-email-templates-table.xml
│   │       │   │   └── 003-create-email-campaigns-table.xml
│   │       │   └── db.changelog-master.xml
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── input.css      # Tailwind source
│   │       │   │   └── output.css     # Generated CSS (gitignored)
│   │       │   ├── js/
│   │       │   └── images/
│   │       ├── templates/        # Thymeleaf templates
│   │       │   └── index.html
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── liquibase.properties (gitignored)
│   │       └── liquibase.properties.template
│   └── test/
│       └── java/com/openmailer/openmailer/
│           └── OpenmailerApplicationTests.java
├── .gitignore
├── mvnw                         # Maven wrapper (Unix)
├── mvnw.cmd                     # Maven wrapper (Windows)
├── package.json                 # NPM dependencies
├── pom.xml                      # Maven configuration
├── tailwind.config.js           # Tailwind configuration
└── README.md
```

---

## Tech Stack

### Backend
- **Spring Boot 4.0** - Application framework
- **Spring Data JPA** - Database ORM
- **Spring Security** - Authentication & authorization
- **Spring Mail** - Email functionality
- **PostgreSQL** - Database
- **Liquibase** - Database migration management

### Frontend
- **Thymeleaf** - Template engine
- **Tailwind CSS 3.4** - Utility-first CSS framework
- **Heroicons** - SVG icons

### Build Tools
- **Maven** - Java dependency management
- **NPM** - Node package management

---

## Database Schema

### Users Table
- User authentication and management
- Fields: id, username, email, password, enabled, created_at, updated_at

### Email Templates Table
- Store reusable email templates
- Fields: id, name, subject, body, description, created_by, created_at, updated_at

### Email Campaigns Table
- Manage bulk email campaigns
- Fields: id, name, template_id, status, scheduled_at, sent_at, total_recipients, sent_count, failed_count, created_by, created_at, updated_at

---

## Useful Maven Commands

### Skip Tests
```bash
./mvnw clean package -DskipTests
```

### Run Specific Test
```bash
./mvnw test -Dtest=YourTestClass
```

### Debug Mode
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### View Dependency Tree
```bash
./mvnw dependency:tree
```

### Update Dependencies
```bash
./mvnw versions:display-dependency-updates
```

### Generate Project Info
```bash
./mvnw site
```

---

## Environment Variables (Production)

For production deployment, set these environment variables:

```bash
DATABASE_URL=jdbc:postgresql://your-host:5432/openmailer
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password
MAIL_HOST=smtp.your-mail-server.com
MAIL_PORT=587
MAIL_USERNAME=your_email@domain.com
MAIL_PASSWORD=your_email_password
```

---

## Troubleshooting

### Port Already in Use
If port 8080 is already in use, change it in `application.properties`:

```properties
server.port=8081
```

### Database Connection Failed
- Verify PostgreSQL is running: `systemctl status postgresql`
- Check credentials in `application-dev.properties`
- Ensure database exists: `psql -l`

### Liquibase Fails
- Verify database connection
- Check `liquibase.properties` configuration
- Clear checksums: `./mvnw liquibase:clearCheckSums`

### Tailwind CSS Not Working
- Ensure output.css is generated: `npm run build:css`
- Check browser console for CSS loading errors
- Verify path in HTML: `/css/output.css`

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## License

This project is open source and available under the [MIT License](LICENSE).

---

## Support

For issues and questions:
- Create an issue in the repository
- Email: support@openmailer.com

---

## Acknowledgments

Built with Spring Boot, PostgreSQL, and Tailwind CSS.
