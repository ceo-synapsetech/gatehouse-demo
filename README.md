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

Deploy the `frontend/` directory as a Cloudflare Pages static site. Before deployment, set `window.GATEHOUSE_API_BASE` to the Render API URL (a `config.js` or a small inline config loaded before the app script can do this). The frontend must use the exact same origin value configured as `CORS_ALLOWED_ORIGIN` on Render.

## GitHub Actions deployment pipeline

The workflow at `.github/workflows/deploy-demo.yml` builds the Spring Boot API on pull requests and pushes to `main`. A push to `main` then deploys the static frontend to Cloudflare Pages and triggers a Render API deploy. It currently runs a Maven package build with test execution skipped; no test suite is included in this demo.

Set these repository **Actions secrets**:

- `CLOUDFLARE_API_TOKEN` (Cloudflare Pages edit permission)
- `CLOUDFLARE_ACCOUNT_ID`
- `GATEHOUSE_API_BASE_URL` (the Render API base URL, e.g. `https://gatehouse-api.onrender.com`)
- `RENDER_DEPLOY_HOOK_URL` (Render service Settings → Deploy Hook)

Set this repository **Actions variable**:

- `CF_PAGES_PROJECT` (the Cloudflare Pages project name)
- `DEPLOY_ENABLED=true` only after all deployment secrets and services have been configured. Until then, the pipeline builds the API but skips deployment.

Connect Render to the same GitHub repository and configure the service to use `backend/Dockerfile` with `backend/` as its Docker context. The GitHub workflow triggers its deploy hook after the frontend deploy; turn off Render's automatic deploy-on-commit to avoid duplicate deployments. Configure Render's `CORS_ALLOWED_ORIGIN` with the deployed Pages origin. Store secrets only in GitHub Actions or the hosting dashboards, never in source files or chat.

## Demo scope and go-live work

This starter is for a client demo, not production. It has two fixed users, one resident account, sample blocks/units, and basic role checks. No push notifications, resident imports, registration, password reset, audit trail, rate limiting, field-level encryption, secrets manager integration, backup/retention policy, or production session/token flow is included. Use only fictitious data. Before go-live, replace demo auth, add required personal-data safeguards and operational processes, configure managed secrets and backups, and obtain the appropriate privacy/legal review for India.
