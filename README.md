# Gatehouse client demo

This is a demo project for a residential security desk workflow. It uses a static HTML frontend, a Spring Boot REST API, and PostgreSQL in a hosted environment. Demo records and accounts are fictitious. Do not enter real resident, guard, or delivery-person personal information.

## Demo accounts

- Security desk: `guard01` / `demo123`
- Resident: `ananya@example.com` / `demo123` (Block A, Unit 204)

HTTP Basic authentication is used only to keep the demo small. The UI keeps credentials in memory and sends them over HTTPS when deployed. Replace this with a production identity/session design before launch. The demo API stores no phone number field.

## Run the API locally

Requirements: Java 21 and Maven 3.9+.

From `backend/` run:

```sh
mvn spring-boot:run
```

The API listens on `http://localhost:8080`; local development uses an in-memory H2 database. Open `frontend/index.html` from a local static server at `http://localhost:8788` (for example, `python -m http.server 8788` from `frontend/`). The frontend is configured to call `http://localhost:8080` by default.

## Render backend deployment

Render supports a Docker-based web service, so it can run the Spring Boot API, and it offers managed PostgreSQL. Create a PostgreSQL database and a Docker Web Service from this folder with the Dockerfile path set to `backend/Dockerfile` and Docker context set to `backend`.

Set these API environment variables in Render:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` from the Render Postgres **internal** connection details
- `CORS_ALLOWED_ORIGIN` to the exact Cloudflare Pages demo origin (for example, `https://your-project.pages.dev`)
- `DDL_AUTO=validate`

Place the API and Postgres in the same Render region and use the internal database host. For a Mumbai demo, Singapore is a nearby region if available in the Render account. Keep credentials in Render environment variables; do not commit them.

## Cloudflare frontend

The Cloudflare Pages project is named `gatehouse-demo` and will be served at `https://gatehouse-demo.pages.dev`. The GitHub Actions workflow writes the Render API URL into `frontend/config.js` during deployment. Render's `CORS_ALLOWED_ORIGIN` must match the Pages origin exactly.

## GitHub Actions deployment pipeline

The workflow at `.github/workflows/deploy-demo.yml` builds the Spring Boot API on pull requests and pushes to `main`. On `main`, it deploys the static frontend to Cloudflare Pages when the `DEPLOY_ENABLED` Actions variable is `true`; you can also run a manual deployment from the Actions tab using `workflow_dispatch`. The workflow uses Cloudflare's `cf` CLI for Pages direct upload. Render is configured to auto-deploy from `main`, so GitHub pushes deploy the API there automatically. The Maven package step skips tests because this demo has no test suite.

Set this repository **Actions secret**:

- `CLOUDFLARE_API_TOKEN` (a token with Cloudflare Pages Edit access scoped to the Gatehouse account)

Set this repository **Actions variable**:

- `DEPLOY_ENABLED=true` after the token is saved. The Cloudflare account ID, Pages project name, and Render API URL are already set in the workflow.

Keep the Cloudflare token in GitHub Actions Secrets; never commit it or paste it into chat.

## Demo scope and go-live work

This starter is for a client demo, not production. It has two fixed users, one resident account, sample blocks/units, and basic role checks. No push notifications, resident imports, registration, password reset, audit trail, rate limiting, field-level encryption, secrets manager integration, backup/retention policy, or production session/token flow is included. Use only fictitious data. Before go-live, replace demo auth, add required personal-data safeguards and operational processes, configure managed secrets and backups, and obtain the appropriate privacy/legal review for India.
