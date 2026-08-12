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

Keep the backend as a single Gradle project with package-level feature
boundaries. Introduce a separate Gradle module only when a feature earns a
stable dependency boundary that justifies the additional build and wiring
complexity.

The package responsibilities are:

- `common`: shared API pagination, error helpers, and stable utilities;
- `users`: users, roles, profile updates, and user-facing persistence;
- `notes`: notes, ownership checks, Markdown rules, and pagination;
- `security`: JWT, token lifecycle, CSRF, CORS, and authorization support;
- `app`: Spring Boot launcher, web wiring, configuration, and OpenAPI.

Keep dependencies directed toward stable lower-level packages and enforce the
rules with ArchUnit tests once cross-feature code exists. Do not place
domain-specific code into `common`.

Use Java 25 toolchains, Spring Data JPA/Hibernate, Liquibase, Lombok, MapStruct,
and JaCoCo. Configure an `integrationTest` source set and package tree. Use
Testcontainers PostgreSQL for integration tests, including Liquibase migrations.
`check` must enforce at least 80% JaCoCo coverage for production code, excluding
generated code and configuration boilerplate.

Apply Spotless with the checked-in Eclipse Java formatter profile and make
`check` depend on `spotlessCheck`, unit tests, `integrationTest`, Liquibase
validation, and JaCoCo verification. The repository owns its formatter profile
to keep IntelliJ IDEA, local Gradle checks, and CI aligned.

Use explicit DTOs at REST boundaries and never expose JPA entities. Controllers
perform binding, validation, mapping, and HTTP semantics only; services own
workflows and persistence behavior. Use constructor injection and centralized
`ProblemDetail` exception handling. Log safe operation context before writes,
never credentials, JWTs, refresh tokens, or complete request payloads.

Keep authentication workflows in dedicated controllers: registration, login,
token refresh, and email verification are not user-resource operations.
Reserve `UserController` for profile and administrator user-resource endpoints.

When the first API DTOs are introduced, configure Jackson globally with
`SNAKE_CASE` for JSON serialization and deserialization. Do not add JSON naming
annotations to JPA entities; use per-DTO overrides only for documented
exceptions.

## Database and migrations

Create Liquibase migrations in schema `vaultnote` for:

- `users`: `BIGINT` ID, unique email, `display_name`, Argon2id password hash,
  verification state, timestamps, and future-compatible auth-provider fields.
- `user_roles`: many-to-many user assignments with stable numeric role codes;
  the initial codes are `USER` (1) and `ADMIN` (2).
- `notes`: owner relation, title, Markdown content, timestamps, and `@Version`.
- `refresh_tokens`: per-session hashed token, expiry, rotation lineage, and
  revocation state.
- `email_verification_tokens`: user relation, single-use hashed token, expiry,
  used timestamp, and creation timestamp for the email-verification MVP.
- `auth_audit_events`: successful/failed login, logout, and token revocation.

The role migration assigns `USER` to existing users. Open registration grants
only `USER`; promote the initial administrator with a documented manual SQL
script that inserts role code `2`. Never add a privileged bootstrap endpoint.

Treat every schema evolution as an ordered, forward-only Liquibase migration;
set Hibernate DDL generation to validation only. Use PostgreSQL-specific SQL
changesets where needed. Put invariants in the schema with explicit constraints
and indexes, and test them against real PostgreSQL through Testcontainers.

## Authentication and security

Implement these flows:

1. Register with email, display name, and a password of at least 12 characters
   including at least two digits. Return a neutral public response.
2. Implement the email-verification MVP after the Mailpit baseline:
   - create an expiring, single-use hashed token;
   - send the raw token in a verification link through `MailSender`;
   - expose a dedicated verification endpoint;
   - atomically mark the token as used and `users.email_verified` as `true`.
   The MVP intentionally does not include outbox delivery, resend, rate
   limiting, token history, or cleanup jobs.
3. Allow verified users to log in. Issue a 15-minute JWT access token returned
   to Angular memory and a 7-day refresh token in an `HttpOnly` cookie. Validate
   the bearer JWT on protected requests and expose a current-user access-check
   endpoint.
4. Store refresh tokens only as hashes. Rotate on every refresh, revoke the
   current session on logout, and clear the refresh-token cookie.
5. When a rotated refresh token is reused, revoke every active token in the
   affected token family and record the security event.

Implement Spring Security's SPA-compatible CSRF protection with
`CookieCsrfTokenRepository`: expose `GET /csrf`, deliver the raw token in the
`XSRF-TOKEN` cookie, and require it in the `X-XSRF-TOKEN` header for login,
refresh, logout, and other state-changing requests. Keep registration and
email verification explicitly excluded until the public form flow has its own
CSRF integration. Configure CORS as an explicit, environment-driven allowlist
with credentials enabled; never use a wildcard origin with cookies.

Complete JWT authorization integration:

- validate the JWT `roles` claim against the `UserRole` enum and reject missing
  or unknown roles before controller execution;
- map the validated JWT `roles` claim to Spring Security authorities;
- enable method security and use `@PreAuthorize` for role and ownership rules;
- introduce a `CurrentUserProvider` abstraction backed by
  `SecurityContextHolder`;
- register the OpenAPI `bearerAuth` security scheme and annotate protected
  endpoints with `@SecurityRequirement`.

Email delivery hardening is a later phase. It includes verification-email
resend, invalidation of older verification tokens, cleanup of expired tokens,
audit events, and an outbox when reliable delivery and retry semantics become
necessary.

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
the current API returns that source Markdown directly. HTML rendering and
sanitization are deferred until the final step of the Angular phase, when a
preview or read-only mode is introduced.
Attachments and images are outside this iteration.

Use `CurrentUserProvider` in note services for current-user ownership checks.

All collection endpoints are paginated. Sort a user's notes by `updatedAt`
descending by default. Do not add search yet. Use optimistic locking and return
`409 Conflict` as `ProblemDetail` if a note is updated from a stale version.

The Profile page displays the email and verification state, permits only
`display_name` updates, and provides logout for the current session. Email
change and global logout are deferred.

The implemented notes baseline currently includes the owner-only REST CRUD
endpoints, explicit request/response DTOs, pagination with `updatedAt DESC` as
the default order, `CurrentUserProvider` ownership checks, and centralized
`NOTE_NOT_FOUND` handling, optimistic locking through `ETag`/`If-Match`, and
PostgreSQL-backed endpoint integration coverage. The remaining notes work is
administrator read-only access. Safe HTML rendering and sanitization are
deferred until a preview or read-only mode is introduced.

## Phase 4 — Minimal Angular

Create `frontend/` as a standalone Angular application with simple local
styles and the generated client for the authentication endpoints.

Implement only the email/password login page and the minimum auth state needed
to hold the access token in memory and call the existing refresh and CSRF
flows. Do not add Notes, profile, administrator, or Markdown screens yet.

## Phase 4.5 — Password management prerequisite

Complete local password management before starting OAuth2/OIDC. This lets a
user who initially signs in through Google establish a second, local login
method without creating a second VaultNote account:

- make `users.password_hash` nullable for passwordless provider accounts;
- add one authenticated `set/change password` use case and endpoint;
- when a password already exists, require and verify `currentPassword`;
- when no password exists, allow the authenticated user to set one without a
  current password;
- validate and hash the new password through the existing password policy and
  `PasswordEncoder`;
- keep password reset through email as a separate unauthenticated flow;
- prevent removing the last available authentication method;
- add unit and PostgreSQL integration coverage for both set and change
  scenarios.

## Phase 5 — OAuth2/OIDC sign-in

After password management is complete and the minimal Angular login flow is
working, add external sign-in through an OAuth2/OIDC provider such as Google or
GitHub:

- add Spring Security OAuth2 Client support and provider configuration;
- implement the authorization redirect and callback flow;
- keep the application stateless after login, using a short-lived
  cookie-based authorization state for the OAuth handshake;
- map a verified provider identity to a local `User`, assigning `USER` by
  default and requiring explicit rules for account linking;
- handle a new provider identity through an explicit onboarding flow rather
  than an opaque login failure: show the verified provider email as read-only,
  collect or confirm `displayName`, create the local `User` with
  `email_verified = true`, `password_hash = null`, and role `USER`, persist the
  provider identity, and then issue VaultNote tokens;
- do not require a phone number for OAuth onboarding; phone-based
  authentication is outside the current VaultNote scope;
- never silently link a new provider identity to an existing local account
  using only a matching email; require an authenticated local login and an
  explicit account-linking action;
- issue VaultNote's existing JWT access token and refresh-token cookie after
  successful provider authentication;
- never place access or refresh tokens in redirect URLs;
- add login buttons and an OAuth callback route in Angular;
- obtain the access token through the existing refresh flow and keep it only in
  frontend memory;
- add provider-mocked integration coverage and a local manual verification flow.

## Phase 6 — Remaining Angular frontend

Complete the rest of the Angular application with lazy-loaded routes, simple
local styles, and no UI component library:

- registration and email verification;
- paginated Notes list and create/edit/delete flows;
- profile and display-name editing;
- read-only administrator users and Notes lists;
- authenticated and administrator route guards;
- access-token attachment, single-flight refresh, CSRF handling, and standard
  `ProblemDetail` error presentation.

As the final step of this phase, add Markdown preview/read rendering:

- parse the raw Markdown returned by the API;
- sanitize generated HTML before inserting it into the DOM;
- allow only safe link protocols such as `http` and `https`.

Generate the TypeScript client from OpenAPI and add focused Angular unit tests
for auth state, HTTP interceptors, route guards, and Notes flows. Defer browser
e2e tests. Never persist access tokens in local storage or session storage.

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

Record decisions that would be expensive or confusing to rediscover. The
initial ADRs now describe decisions that are implemented in the repository:

1. [x] Monorepo layout (`001`).
2. [x] Antora documentation (`002`).
3. [x] Backend package boundaries (`003`, superseded by `006`).
4. [x] PostgreSQL and Liquibase persistence baseline (`004`).
5. [x] Users persistence ownership (`005`).
6. [x] Single-module modular monolith (`006`).
7. [x] Liquibase and Testcontainers integration-test strategy (`007`).
8. [x] Mail delivery boundary and MVP email verification delivery (`008`).
9. [x] Fixed role model with numeric enum codes and read-only `ADMIN` (`009`).
10. [x] JWT access and initial refresh-token delivery (`010`).
11. [x] Refresh-token rotation and reuse detection (`011`).
12. [x] Current-session logout (`012`).
13. [x] CSRF and CORS strategy (`013`).

The following ADRs remain planned. Proposed ADRs may capture an important
upcoming decision before implementation and become accepted after verification:

- [ ] OpenAPI as the contract source and generated Angular client.
- [ ] Antora publishing to GitHub Pages.
- [ ] OAuth2/OIDC sign-in, provider identity mapping, and handoff to VaultNote
  tokens (`014`, proposed).

## Verification sequence

1. Run backend unit tests, integration tests, Liquibase validation, and JaCoCo
   gate through `./gradlew check`.
2. Run Angular unit tests and production build.
3. Start Mailpit, backend, and frontend locally.
4. Manually verify registration, email confirmation, login, refresh, logout,
   session reuse detection, notes ownership, admin read-only access, CSRF, CORS,
   pagination, and optimistic-lock conflicts.

## Deferred ideas

- Password reset with single-use expiring tokens.
- `SUPER_ADMIN`, tenant-aware roles, and granular permissions.
- Note sharing through explicit per-note permissions.
- Note trash, restore, and search.
- Editing email, logout from all devices, and session management UI.
- Complete audit records for privileged data access.
- A dedicated least-privilege DB user for production-like setups.
- Browser e2e tests, application CI, production deployment, TLS, and real SMTP.

## Final priority — Rate limiting

Implement rate limiting only after the core product, frontend, and deployment
work are complete. Protect login and verification-email resend from abuse with
IP- and email-aware limits, without locking user accounts. Prefer Cloudflare or
another edge/WAF rule for public traffic; add application-level limits only for
direct-origin or internal traffic, or for email-specific rules the edge cannot
enforce.

Add authentication audit events in the same final hardening phase. Cover
successful and failed login, refresh-token reuse, logout, and email-verification
security events without coupling the audit trail to authorization checks.

Add refresh-token cleanup in the same final hardening phase. Remove records only
after `expires_at`; retain revoked but not-yet-expired records so reuse
detection continues to work. The cleanup may run as a scheduled job or an
explicit maintenance task.
