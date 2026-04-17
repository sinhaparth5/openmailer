# OpenMailer Production Deploy Guide

This document describes the intended VPS deployment model for OpenMailer:

- `SafeLine` runs separately as the public WAF / reverse proxy
- `PostgreSQL` runs as a permanent Docker container
- `Redis` runs as a permanent Docker container
- `OpenMailer app` is the only container updated by CI/CD
- the app joins a pre-created external Docker network so it can reach Postgres and Redis

---

## Production Layout

Containers you manage manually on the VPS:

- `openmailer-postgres`
- `openmailer-redis`

Container updated by GitHub Actions:

- `openmailer-app`

Expected Docker network:

- `openmailer-runtime`

Expected app bind:

- `127.0.0.1:8080`

SafeLine should proxy to:

- `127.0.0.1:8080`

---

## 1. Create The Docker Network

Create the shared runtime network once:

```bash
docker network create openmailer-runtime
```

Verify it:

```bash
docker network ls | grep openmailer-runtime
```

---

## 2. Create PostgreSQL Container

Create a persistent volume first:

```bash
docker volume create openmailer_postgres_data
```

Run PostgreSQL:

```bash
docker run -d \
  --name openmailer-postgres \
  --restart unless-stopped \
  --network openmailer-runtime \
  -e POSTGRES_DB=openmailer \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=change-this-postgres-password \
  -v openmailer_postgres_data:/var/lib/postgresql/data \
  postgres:17-alpine
```

Check it:

```bash
docker ps | grep openmailer-postgres
docker logs openmailer-postgres
```

Optional health check:

```bash
docker exec -it openmailer-postgres pg_isready -U postgres -d openmailer
```

---

## 3. Create Redis Container

Create a persistent volume:

```bash
docker volume create openmailer_redis_data
```

Run Redis:

```bash
docker run -d \
  --name openmailer-redis \
  --restart unless-stopped \
  --network openmailer-runtime \
  -v openmailer_redis_data:/data \
  redis:8-alpine redis-server --appendonly yes
```

Check it:

```bash
docker ps | grep openmailer-redis
docker logs openmailer-redis
```

Optional ping test:

```bash
docker exec -it openmailer-redis redis-cli ping
```

If you want Redis password protection later, update the container command and matching env values in `openmailer.env`.

---

## 4. Create Deployment Folder

Create a directory for the deployed app compose file and env:

```bash
mkdir -p /opt/openmailer
```

This path should match the GitHub Actions secret:

- `DEPLOY_APP_PATH=/opt/openmailer`

---

## 5. Create `openmailer.env`

Inside the VPS deploy folder, create:

```bash
/opt/openmailer/openmailer.env
```

You can start from the repo example:

- [`deploy/openmailer.env.example`](/home/tabish/testing-folder/openmailer/deploy/openmailer.env.example:1)

Example production file:

```env
OPENMAILER_IMAGE=ghcr.io/your-github-user/openmailer:latest

APP_BASE_URL=https://mail.example.com
APP_BIND_IP=127.0.0.1
APP_HOST_PORT=8080
DOCKER_NETWORK=openmailer-runtime

DATABASE_URL=jdbc:postgresql://openmailer-postgres:5432/openmailer
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=change-this-postgres-password

REDIS_HOST=openmailer-redis
REDIS_PORT=6379
REDIS_USERNAME=
REDIS_PASSWORD=

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
APP_MAIL_FROM=no-reply@example.com
APP_TEST_EMAIL_RECIPIENT=

ENCRYPTION_KEY=replace-with-a-32-char-secret-key
JWT_SECRET=replace-with-a-long-random-secret
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

Important notes:

- `DATABASE_URL` should point to the Postgres container name on the Docker network
- `REDIS_HOST` should point to the Redis container name on the Docker network
- `APP_BASE_URL` must be the real public URL behind SafeLine
- `APP_BIND_IP=127.0.0.1` keeps the app private to the VPS
- `APP_HOST_PORT=8080` is the local port SafeLine should proxy to

---

## 6. Compose File Used By CI/CD

The production compose file is:

- [`deploy/compose.prod.yml`](/home/tabish/testing-folder/openmailer/deploy/compose.prod.yml:1)

This file only starts:

- `openmailer-app`

It assumes:

- Postgres already exists
- Redis already exists
- Docker network already exists
- `openmailer.env` already exists

---

## 7. Manual First Deploy

Before enabling CI/CD, you can test the app manually on the VPS.

Copy the compose file into `/opt/openmailer/compose.yml`, then run:

```bash
cd /opt/openmailer
docker compose --env-file openmailer.env -f compose.yml up -d
```

Check:

```bash
docker ps | grep openmailer-app
docker logs openmailer-app
```

Confirm the app responds locally:

```bash
curl -I http://127.0.0.1:8080/login
```

---

## 8. SafeLine Upstream

In SafeLine, configure the protected application upstream to:

- host: `127.0.0.1`
- port: `8080`

Your public hostname should match:

- `APP_BASE_URL`

Example:

- public URL: `https://mail.example.com`
- local upstream: `http://127.0.0.1:8080`

---

## 9. GitHub Secrets Needed

Add these repository secrets:

- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_APP_PATH`

Notes:

- `DEPLOY_USER` should be the dedicated VPS user allowed to manage Docker
- `GHCR_TOKEN` should be a token that can pull/push packages in GHCR
- `DEPLOY_APP_PATH` should be the folder containing `compose.yml` and `openmailer.env`

Workflow file:

- [`.github/workflows/deploy-prod.yml`](/home/tabish/testing-folder/openmailer/.github/workflows/deploy-prod.yml:1)

---

## 10. Dedicated Deploy User

Create a deploy user on the VPS if you want CI/CD isolated from your main shell user.

Example:

```bash
sudo adduser deploy-openmailer
sudo usermod -aG docker deploy-openmailer
```

Then install that user's SSH key into:

```bash
/home/deploy-openmailer/.ssh/authorized_keys
```

Use that account for:

- `DEPLOY_USER`

After changing Docker group membership, either log out and back in or restart the session.

---

## 11. Updating Environment Values Later

To change production config:

1. edit `/opt/openmailer/openmailer.env`
2. redeploy the app

Manual redeploy:

```bash
cd /opt/openmailer
docker compose --env-file openmailer.env -f compose.yml up -d
```

If the image changed too:

```bash
cd /opt/openmailer
docker compose --env-file openmailer.env -f compose.yml pull
docker compose --env-file openmailer.env -f compose.yml up -d
```

---

## 12. Useful Checks

Check running containers:

```bash
docker ps
```

Check app logs:

```bash
docker logs -f openmailer-app
```

Check Postgres logs:

```bash
docker logs -f openmailer-postgres
```

Check Redis logs:

```bash
docker logs -f openmailer-redis
```

Check app network attachment:

```bash
docker inspect openmailer-app
```

Check the Docker network:

```bash
docker network inspect openmailer-runtime
```

---

## 13. Summary

Permanent VPS containers:

- `openmailer-postgres`
- `openmailer-redis`

Permanent VPS network:

- `openmailer-runtime`

CI/CD-updated container:

- `openmailer-app`

SafeLine upstream:

- `127.0.0.1:8080`
