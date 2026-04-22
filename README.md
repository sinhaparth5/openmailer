# OpenMailer

OpenMailer is a self-hosted email platform built with Spring Boot, PostgreSQL, Redis, Thymeleaf, and Tailwind CSS. Test

## Overview

OpenMailer is designed for teams that want to run email operations on their own infrastructure instead of relying fully on third-party SaaS tools.

It separates a few core concerns:

- application logic and UI in Spring Boot
- persistent data in PostgreSQL
- transient/cache workloads in Redis
- reverse proxy and TLS at the VPS edge

## Core Concepts

- Templates define reusable email content.
- Campaigns use templates plus recipients to send bulk email.
- Domains control verified sender identities and DNS records.
- Providers handle outbound delivery such as SMTP or external email services.

## Stack

- Java 25
- Spring Boot
- PostgreSQL
- Redis
- Thymeleaf
- Tailwind CSS
- Docker Compose

## Local Development

Install dependencies:

```bash
npm install
```

Run the app:

```bash
./mvnw spring-boot:run
```

Run Tailwind in watch mode:

```bash
npm run watch:css
```

App URL:

```text
http://localhost:8080
```

## Build

Compile:

```bash
./mvnw compile
```

Run tests:

```bash
./mvnw test
```

Package:

```bash
./mvnw clean package
```

## Production

Deploys are designed for a VPS with Docker Compose.

Main files:

- `deploy/compose.prod.yml`
- `.github/workflows/deploy-prod.yml`
- `architecture/DEPLOY.md`

On the VPS, the app runs behind a reverse proxy and is exposed internally on:

```text
127.0.0.1:8080
```

In production, the usual request flow is:

```text
Client -> reverse proxy / WAF -> OpenMailer app -> PostgreSQL / Redis
```

## Important Paths

- `src/main/java` — backend code
- `src/main/resources/templates` — Thymeleaf views
- `src/main/resources/static` — CSS, JS, images
- `src/test/java` — tests
- `architecture` — deployment and progress docs

## Notes

- Use `openmailer.env` on the VPS for production environment variables.
- CI/CD should update only `openmailer-app`.
- Custom error pages live under `src/main/resources/templates/error`.
