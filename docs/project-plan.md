# VaultNote implementation plan

## Goal

Build a local learning application that exercises Spring Security, Angular,
PostgreSQL, API contracts, and a realistic session lifecycle.

The first iteration is a private Markdown notes application with local
email/password sign-in, email verification, JWT access tokens, and roles.

## Scope and boundaries

- The repository contains `backend/` and `frontend/`.
- The UI and API use English.
- The local PostgreSQL server already owns database `vault_note` and schema
  `vaultnote`. The existing database user is sufficient for local work.
- Docker is used only for Mailpit. PostgreSQL stays in the shared local setup.
- Application CI and deployment are not part of the first iteration. A small
  docs-only GitHub Actions workflow publishes the documentation to Pages.
- OpenAPI is the API source of truth; Angular uses a generated TypeScript
  client.
- API errors use RFC 9457 `ProblemDetail`.
- Architecture decisions are recorded as ADRs under
  `docs/modules/ROOT/pages/adr/`.

## Backend structure

Move the current Gradle build into `backend/` and make it a multi-module build.

| Module | Responsibility |
| --- | --- |
| `common` | Shared API pagination, error helpers, and stable utilities. |
| `users` | Users, roles, profile updates, and user-facing persistence. |
| `notes` | Notes, ownership checks, Markdown rules, and pagination. |
| `security` | JWT, token lifecycle, CSRF, CORS, and authorization support. |
| `app` | Spring Boot launcher, web wiring, configuration, and OpenAPI. |

Keep dependencies directed toward stable lower-level modules. Do not place
domain-specific code into `common`.

Use Java 25 toolchains, Spring Data JPA/Hibernate, Liquibase, Lombok, MapStruct,
and JaCoCo. Configure an `integrationTest` source set and package tree. Use
Testcontainers PostgreSQL for integration tests, including Liquibase migrations.
`check` must enforce at least 80% JaCoCo coverage for production code, excluding
generated code and configuration boilerplate.

Apply Spotless with Palantir Java Format, as required by the global engineering
standards, and make `check` depend on `spotlessCheck`, unit tests,
`integrationTest`, Liquibase validation, and JaCoCo verification. Adopt the
same formatter version specified in the shared standards directly in this build;
this repository intentionally does not consume the standards as a submodule.

Use explicit DTOs at REST boundaries and never expose JPA entities. Controllers
perform binding, validation, mapping, and HTTP semantics only; services own
workflows and persistence behavior. Use constructor injection and centralized
`ProblemDetail` exception handling. Log safe operation context before writes,
never credentials, JWTs, refresh tokens, or complete request payloads.

When the first API DTOs are introduced, configure Jackson globally with
`SNAKE_CASE` for JSON serialization and deserialization. Do not add JSON naming
annotations to JPA entities; use per-DTO overrides only for documented
exceptions.

## Database and migrations

Create Liquibase migrations in schema `vaultnote` for:

- `users`: `BIGINT` ID, unique email, `display_name`, Argon2id password hash,
  verification state, timestamps, and future-compatible auth-provider fields.
- `roles` and `user_roles`: initial roles are `USER` and `ADMIN`.
- `notes`: owner relation, title, Markdown content, timestamps, and `@Version`.
- `refresh_tokens`: per-session hashed token, expiry, rotation lineage, and
  revocation state.
- `email_verification_tokens`: expiring, single-use hashed tokens.
- `auth_audit_events`: successful/failed login, logout, and token revocation.

Seed `USER` and `ADMIN` roles via migrations. Open registration grants only
`USER`. Promote the initial administrator with a documented manual SQL script;
never add a privileged bootstrap endpoint.

Treat every schema evolution as an ordered, forward-only Liquibase migration;
set Hibernate DDL generation to validation only. Use PostgreSQL-specific SQL
changesets where needed. Put invariants in the schema with explicit constraints
and indexes, and test them against real PostgreSQL through Testcontainers.

## Authentication and security

Implement these flows:

1. Register with email, display name, and a password of at least 12 characters
   including at least two digits. Return a neutral public response.
2. Issue a one-time, expiring email-verification link through Mailpit. Support
   rate-limited resend; invalidate older verification tokens.
3. Allow verified users to log in. Issue a 15-minute JWT access token returned
   to Angular memory and a 7-day refresh token in an `HttpOnly` cookie.
4. Store refresh tokens only as hashes. Rotate on every refresh and revoke the
   current session on logout.
5. When a rotated refresh token is reused, revoke every active session for the
   affected user and record the security event.

Protect login by rate limiting on both IP and email, without account lockout.
Implement CSRF protection for cookie-backed refresh and logout calls using the
Angular-compatible XSRF token pattern. Configure CORS as an explicit allowlist
for the local Angular origin with credentials enabled.

Use production-like cookie settings where possible. The `local` profile may omit
the `Secure` flag for HTTP localhost; document the required production setting.

## Authorization

- `USER` can read, create, update, and permanently delete only owned notes.
- `ADMIN` can read paginated lists of all users and all notes, but cannot alter
  users, roles, or others' notes.
- Backend service-level ownership and role checks are mandatory. Angular guards
  and hidden UI controls are user experience only.
- Notes are private. Sharing is outside this iteration.

## Notes and profile

Build CRUD for notes with `BIGINT` IDs, title limited to 200 characters, and
Markdown content limited to 20,000 characters. Store source Markdown only;
disallow raw HTML, attachments, and images. Render a safe Markdown subset,
sanitize output, and permit only safe link protocols.

All collection endpoints are paginated. Sort a user's notes by `updatedAt`
descending by default. Do not add search yet. Use optimistic locking and return
`409 Conflict` as `ProblemDetail` if a note is updated from a stale version.

The Profile page displays the email and verification state, permits only
`display_name` updates, and provides logout for the current session. Email
change and global logout are deferred.

## Angular frontend

Create `frontend/` as a standalone Angular application with lazy-loaded routes,
simple local styles, and no UI component library.

Implement screens for:

- registration and email verification;
- login;
- paginated notes list and create/edit/delete flows;
- profile and display-name editing;
- read-only admin users and notes lists.

Add route guards for authenticated and administrator routes. Add HTTP logic for
access-token attachment, a single-flight refresh flow, CSRF handling, and
standard `ProblemDetail` error presentation. Never persist access tokens in
local storage or session storage.

Generate the TypeScript client from OpenAPI. Add focused Angular unit tests for
auth state, HTTP interceptors, and route guards; defer browser e2e tests.

Keep Java tests deterministic and behavior-focused. Use AssertJ for assertions,
Mockito only for direct collaborators, and a shared PostgreSQL Testcontainer for
the integration-test JVM. Reset test data deterministically; never depend on
test order or arbitrary sleeps.

## Local developer experience

1. Add `compose.yaml` for Mailpit and document its SMTP and web UI ports.
2. Add `.env.example` with non-secret configuration names; ignore `.env` and
   local backend secret files.
3. Provide `local` configuration for PostgreSQL, Mailpit, Angular origin, and
   safe local cookie behavior.
4. Provide `test` configuration driven by Testcontainers, not the shared DB.
5. Write a README with database prerequisites, startup commands, API docs, the
   manual admin-role SQL step, and the main verification checklist.
6. Use Antora for docs-as-code. Keep a versionless VaultNote component under
   `docs/`, with `antora.yml`, `modules/ROOT/pages/`, and `modules/ROOT/nav.adoc`.
7. Create `docs/modules/ROOT/pages/adr/` with short, numbered Architecture
   Decision Records. Each ADR states context, decision, consequences, and
   status.
8. Add a GitHub Actions workflow that builds the Antora site and deploys the
   static artifact to GitHub Pages after changes to documentation reach `main`.

## ADR status

Record decisions that would be expensive or confusing to rediscover. The first
five ADRs now describe decisions that are implemented in the repository:

1. [x] Monorepo layout (`001`).
2. [x] Antora documentation (`002`).
3. [x] Backend module boundaries (`003`).
4. [x] PostgreSQL and Liquibase persistence baseline (`004`).
5. [x] Users persistence ownership (`005`).

The following ADRs remain planned and must be created only when their decisions
are implemented:

- [ ] JWT access token and rotating refresh token in an `HttpOnly` cookie.
- [ ] CSRF and CORS strategy for Angular and cookie-backed refresh operations.
- [ ] Role model with `USER` and read-only `ADMIN`.
- [ ] OpenAPI as the contract source and generated Angular client.
- [ ] Liquibase and Testcontainers integration-test strategy.
- [ ] Antora publishing to GitHub Pages.

## Verification sequence

1. Run backend unit tests, integration tests, Liquibase validation, and JaCoCo
   gate through `./gradlew check`.
2. Run Angular unit tests and production build.
3. Start Mailpit, backend, and frontend locally.
4. Manually verify registration, email confirmation, login, refresh, logout,
   session reuse detection, notes ownership, admin read-only access, CSRF, CORS,
   pagination, optimistic-lock conflicts, and Markdown XSS resistance.

## Deferred ideas

- Password reset with single-use expiring tokens.
- OAuth2 sign-in.
- `SUPER_ADMIN`, tenant-aware roles, and granular permissions.
- Note sharing through explicit per-note permissions.
- Note trash, restore, and search.
- Editing email, logout from all devices, and session management UI.
- Complete audit records for privileged data access.
- A dedicated least-privilege DB user for production-like setups.
- Browser e2e tests, application CI, production deployment, TLS, and real SMTP.
