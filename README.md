# iBatch Financial Operations

iBatch is a full-stack batch-processing application for loading, validating, processing, and auditing financial transaction CSV files. It keeps the operator-controlled workflow: a file is placed in a configurable input directory, selected, validated, processed in batches, and persisted with traceable accepted and rejected results.

## Features

- Spring Security login with an HttpOnly session cookie and CSRF protection.
- Drag-and-drop or picker-based CSV upload.
- Discovery of files already in the configured input directory.
- Validation of `transactions_DDMMYYYY.csv`, headers `account,amount,date`, size, emptiness, and content.
- Asynchronous processing with progress, JDBC batch writes, and configurable record limits.
- Validation of 10-digit accounts, positive amounts, dates, and duplicate `account + date + amount` records.
- Processed/rejected views, rejection reasons, eligible amount correction/reprocessing, dashboard, and audit log.

## Architecture

```text
Next.js browser ---> Spring Boot API ---> MySQL
                         |
                         +--> configurable /data/input directory
```

The upload stores the file atomically in the same input directory used by file discovery; processing remains a separate operator action.

## Current deployment status

The intended architecture is Vercel (Next.js) -> Render (Spring Boot) -> Aiven (MySQL 8.4).

- Frontend: [i-batch.vercel.app](https://i-batch.vercel.app)
- Intended backend: [ibatch-backend.onrender.com](https://ibatch-backend.onrender.com)
- Database: Aiven MySQL with TLS required.
- Flyway creates and versions the relational model at startup.

**Verification status (September 2026):** the frontend repository and application code are present, but the Render backend is not healthy. Recent deployments fail during Flyway startup with a MySQL `Communications link failure` to the configured Aiven endpoint. This is an infrastructure/database-connectivity blocker, not a verified production deployment. The separate Render service `ibatch-backend-deploy` is a duplicate/legacy service and should not be used as the canonical backend.

Before production approval, restore or replace the Aiven database, verify the Render secrets `DB_PASSWORD` and `APP_AUTH_PASSWORD`, then confirm `GET /api/health` and `GET /api/health/database`. Free Render also sleeps after inactivity and has ephemeral local disk; production use should add object storage or a persistent disk for uploaded CSV files.

### Production variables

`DB_PASSWORD` and `APP_AUTH_PASSWORD` are secrets. Configure Vercel with:

```dotenv
NEXT_PUBLIC_API_BASE_URL=https://ibatch-backend.onrender.com
```

Redeploy Vercel after changing any `NEXT_PUBLIC_*` value because it is embedded at build time. Use `SESSION_COOKIE_SECURE=true`, `SESSION_COOKIE_SAME_SITE=none`, and exact CORS origins for cross-domain HTTPS.

## Local Docker Compose

Requirements: Docker Compose v2, ports 3000 and 8080, and persistent storage.

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Local URLs: frontend `http://localhost:3000`; backend health `http://localhost:8080/api/health`. Never commit `.env` or real credentials. A new MySQL volume runs the scripts in `database/` in order.

## CSV workflow

1. Sign in.
2. Select or drop a CSV.
3. Validate filename, date, extension, size, headers, emptiness, collisions, and path.
4. Move it atomically to `/data/input`.
5. Confirm processing.
6. Persist accepted/rejected records and audit events.

Example:

```csv
account,amount,date
2000000000,3241.71,31/07/2026
```

## Security and endpoints

Login passwords are BCrypt-encoded; functional endpoints require a session; mutations require `X-XSRF-TOKEN`; CORS allows only the configured frontend origin; HTTPS and `SESSION_COOKIE_SECURE=true` are required in production.

Public endpoints: `GET /auth/csrf`, `POST /auth/login`, `GET /api/health`, `GET /api/health/database`.

Authenticated endpoints include `/auth/me`, `/auth/logout`, `/files/upload`, `/files/available`, `/files/process`, `/files/{id}`, `/files/{id}/progress`, `/transactions/{id}`, dashboard, and audit routes.

## Local verification

```powershell
cd backend; mvn test
cd ../frontend; npm ci; npm run lint; npm run build
cd ..; docker compose config; docker compose build
```

Before production approval, verify health, login/logout, CSRF rejection, valid/invalid CSV cases, processing progress, rejection/reprocessing, dashboard counts, audit logs, restart persistence, backups, CORS, HTTPS, and absence of secrets in Git.