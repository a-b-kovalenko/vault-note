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
to keep IntelliJ IDEA, local Gradle checks, and CI aligned. The profile uses a
120-character line limit and wraps long annotation arguments on demand, so
`@ApiResponse`, `@SortDefault`, and DTO annotations follow the same rule.

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
  verification state, and timestamps. Phase 7 makes `password_hash` nullable
  for accounts that initially use only an external provider.
- `user_avatars` (Phase 6): one optional avatar per user, stored separately from
  the user row with normalized image bytes, content type, size, and timestamps.
  Keep the storage behind an application interface so the PostgreSQL-backed MVP
  can later move to shared object storage without changing the profile API.
- `user_roles`: many-to-many user assignments with stable numeric role codes;
  the initial codes are `USER` (1) and `ADMIN` (2).
- `notes`: owner relation, title, Markdown content, timestamps, and `@Version`.
- `refresh_tokens`: per-session hashed token, expiry, rotation lineage, and
  revocation state.
- `email_verification_tokens`: user relation, single-use hashed token, expiry,
  used timestamp, and creation timestamp for the email-verification MVP.
- `oauth_identities` (Phase 7): user relation, a stable provider value such as
  `GOOGLE` or `GITHUB`, the provider's stable subject identifier, and creation
  timestamp. Store the provider enum as a stable string and enforce unique
  `(provider, provider_subject)` and `(user_id, provider)` combinations.
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
   endpoint. Keep access authentication stateless while persisting the refresh
   session for rotation and revocation.
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

Distinguish stateless access authentication from application state. The
backend must not use `HttpSession` or a server-side login session as the source
of access authentication, but login, refresh, and logout still have side
effects: they create, rotate, revoke, or clear the refresh session. CSRF
protects those state-changing browser requests; it is not a requirement for
creating a Spring HTTP session.

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

### Future verification-email resend design

The email-verification MVP intentionally sends one message during registration
and does not expose a resend operation. When resend is added, keep it as an
explicit user action rather than an automatic retry. A page refresh must not
send another email, and the browser must not repeatedly call the endpoint on
page load.

The proposed endpoint is:

```http
POST /api/v1/auth/email-verification/resend
Content-Type: application/json

{ "email": "user@example.com" }
```

The endpoint should return `202 Accepted` with the same neutral response when
the email is unknown, already verified, or belongs to an unverified local
account. This prevents account enumeration. A syntactically invalid email may
still return the normal request-validation error; the response must not reveal
whether a valid address belongs to a user.

Possible user-triggered entry points are:

- the success state on the registration page, where the submitted email can be
  pre-filled and the user can click `Resend verification email`;
- the invalid or expired-link state on `/verify-email`, where the user can enter
  an email and request a new link;
- a future login hint for an unverified account, but only if the API exposes a
  non-enumerating contract for that flow.

The frontend should reuse one inline resend component or state model in these
existing screens. It should show a neutral confirmation, preserve the email
input, disable the button while the request is running, and use `Retry-After`
from a `429` response to display a countdown. It should not create a separate
resend page unless a later product decision requires a dedicated recovery
flow.

The controller should only bind the request and pass the client context to an
application service. The service flow should be:

1. Normalize the email and apply a dedicated rate-limit scope for verification
   resend by IP and normalized email before looking up the user.
2. For an unknown or already verified email, return the same generic result
   without generating a token or sending a message.
3. For an unverified local account, invalidate its active verification tokens,
   generate a new cryptographically random token, persist only its hash with a
   new expiry, and send the raw token only in the email link.
4. Return the same neutral `202 Accepted` response for every accepted request.

The rate-limit attempt should be consumed even when no user is found, and the
failure response must not identify whether the IP or email limit was reached.
The implementation can reuse the existing `email_verification_tokens` table
and token-hashing rules; it should not introduce a second token format or log
raw tokens and complete email addresses. Add unit and PostgreSQL integration
coverage for token invalidation, generic responses, unknown and verified
emails, email delivery for an unverified account, IP/email limits, `429`,
`Retry-After`, and the guarantee that page refresh does not trigger a resend.

Use production-like cookie settings where possible. The `local` profile may omit
the `Secure` flag for HTTP localhost; document the required production setting.

## Phase 3 — Minimal Angular

Create `frontend/` as a standalone Angular application with simple local
styles and the generated client for the authentication endpoints.

Implement the minimal authentication surface: public email/password
registration, email verification, the email/password login page, the
authenticated `/me` screen, and current-session logout. Registration sends a
display-name-personalized verification email; `/verify-email` consumes the
one-time token and exposes loading, success, and invalid-link states before
returning the user to login. Keep the access token in memory and use the
existing CSRF, bearer, and refresh flows. The resulting model is stateless
access authentication with a stateful rotating refresh session: no
`HttpSession`, a per-request `SecurityContext`, an access JWT in Angular
memory, and a hashed refresh session in PostgreSQL behind an `HttpOnly` cookie.
Do not add Notes, profile editing, administrator, or Markdown screens yet.

## Phase 4 — Password management prerequisite

Finish the unauthenticated email password-recovery flow before starting
OAuth2/OIDC. This phase covers existing local accounts and also verifies an
unverified email after a successful reset.

The implemented backend recovery baseline is a separate unauthenticated email
flow:

### Password recovery backend

1. `POST /api/v1/auth/password-reset/request` always returns `202 Accepted`
   without account-specific data, whether the account exists or is verified.
   This prevents account enumeration.
2. For an existing local account, invalidate older active reset records,
   generate a cryptographically random one-time token, store only its hash with
   expiry and used-at metadata, and send the raw token only in a Mailpit/email
   link to `/reset-password`. The raw token is never logged.
3. `POST /api/v1/auth/password-reset/confirm` validates the token hash, expiry,
   invalidation, and one-time state under a pessimistic lock, then applies the
   existing password policy and `PasswordEncoder`.
4. A successful reset marks the token used, sets the local password,
   confirms an unverified email, invalidates active email-verification tokens,
   revokes all active refresh sessions, clears the presented refresh cookie,
   and requires a fresh login. Access JWTs are not placed in the email link or
   reset response.
5. Invalid or expired tokens return the stable `PASSWORD_RESET_FAILED` error
   without revealing account information.

### Password recovery frontend

The Angular forgot/reset-password pages, fresh-request path, and removal of the
reset token from the address bar after a successful confirmation are complete.
Before OAuth, the remaining confirmed security-audit findings are handled in
Phase 5.

## Phase 5 — Security audit remediation

The defensive audit found one HIGH finding and three MEDIUM findings. `HIGH-1`,
`MEDIUM-1`, and `MEDIUM-2` are resolved. `MEDIUM-3` is in progress: login,
registration, and password reset are protected by bounded local IP- and
normalized-email-aware limits, and the Angular auth screens show a shared
rate-limit notice/countdown. Shared storage and multi-instance deployment
policy remain. The remaining work is ordered by authentication/session
correctness before adding another authentication provider:

`MEDIUM-3` is therefore only partially resolved: the local/single-instance
scope is complete, while the full finding closes after Phase 11 evaluates and
validates the shared-store choice (PostgreSQL or Redis), then completes the
shared storage, edge/WAF, and deployment-hardening tasks.

### Security remediation backend

1. [x] (`HIGH-1`) Remove the known JWT fallback, require an explicit secret, and
   document the stable local profile secret.
2. [x] (`MEDIUM-1`) Make refresh-token family revocation commit independently
   when reuse detection returns an authentication error. Add PostgreSQL
   coverage that verifies the committed revoked state.
3. [x] (`MEDIUM-2`) Use the configured refresh-cookie name consistently in
   login, refresh, logout, and cookie clearing. Add extractor unit coverage,
   startup validation, and integration coverage with a non-default cookie name.
4. [x] (`MEDIUM-3`, local scope) Define and implement rate limiting for login,
   registration, and password reset. The local in-memory limiter is suitable
   for one instance. The shared provider is not selected yet: PostgreSQL and
   Redis remain candidates, while provider validation and edge/WAF hardening
   are tracked in Phase 11. Do not lock accounts permanently or reveal account
   existence.
   - [x] Protect login by IP and normalized email before database access and
     Argon2, with bounded local storage, `429`, and `Retry-After`.
   - [x] Protect registration by IP and normalized email/device quota.
   - [x] Protect password reset by IP and normalized email.

### Security remediation frontend

- [x] Add one shared Angular rate-limit notice/countdown to login, registration,
  and forgot-password screens; preserve form values, disable submit during the
  countdown, and expose `Retry-After` through CORS.

OAuth starts after the refresh-session correctness findings `MEDIUM-1` and
`MEDIUM-2` are closed. The remaining rate-limiting scopes and deployment-
sensitive checks must be complete before exposing the backend to an untrusted
network.

## Phase 6 — Profile and user administration

### Backend

- [ ] Implement the profile resource. The API displays the email and
  verification state, permits only `display_name` updates, and uses `GET` and
  `PATCH /api/v1/users/me`; email changes and global logout remain deferred.
- [ ] Add optional profile avatar upload and removal. Use
  `PUT /api/v1/users/me/avatar`, `GET /api/v1/users/me/avatar`, and
  `DELETE /api/v1/users/me/avatar`; keep the image endpoint authenticated and
  user-scoped. Accept only decoded JPEG, PNG, or WebP images, enforce a small
  upload and dimension limit, strip metadata, and normalize the stored image on
  the server. Never trust the filename or client-provided content type.
- [x] Implement the backend current-session logout endpoint.
- [x] Add the paginated administrator user list API.

### Frontend

- [ ] Add an authenticated application shell with a compact account menu that
  shows the user's initials or avatar, display name, and email. Include Profile,
  a conditional Admin users link for `ADMIN`, and a separated Log out action.
- [ ] Implement the Profile screen with read-only email and verification state,
  editable `displayName`, and explicit save/cancel states.
- [ ] Add avatar preview, upload, replacement, removal, and initials fallback.
- [ ] Add the read-only Admin users screen using the existing paginated API.

Backend service-level role checks are mandatory. Angular guards and hidden UI
controls are user experience only; the backend remains the authorization source.

## Phase 7 — OAuth2/OIDC sign-in

After password recovery, the minimal Angular login flow, and the core security
remediation are working, add external sign-in through an OAuth2/OIDC provider
such as Google or GitHub in this order:

### OAuth backend

1. Prepare the account model for provider/passwordless users:
   make `users.password_hash` nullable and add the `oauth_identities` table.
   Store `provider` (`GOOGLE`, `GITHUB`, and future providers) together with
   `provider_subject`, `user_id`, and timestamps. Enforce unique
   `(provider, provider_subject)` and `(user_id, provider)` constraints.
2. Implement authenticated password management:
   add set/change-password use cases and endpoint; require `currentPassword`
   when changing an existing password; allow setting a password when none
   exists; reuse the existing password policy and `PasswordEncoder`.
3. Enforce the account-recovery invariant:
   prevent removing the last available authentication method, so a user keeps
   either a local password or at least one linked provider identity. Add unit
   and PostgreSQL integration coverage for set, change, and invariant cases.
4. Configure Spring Security OAuth2 Client and the selected provider, then
   implement the authorization redirect and callback with a short-lived,
   cookie-based authorization state compatible with the stateless security
   model.
5. Resolve provider identity and onboarding:
   map a verified identity to a local `User`, assign `USER` by default, and for
   a new identity collect or confirm `displayName`, create the user with
   `email_verified = true` and `password_hash = null`, and persist the provider
   identity. Existing accounts require explicit authenticated linking; matching
   email alone must never link accounts.
6. Complete the security handoff:
   issue VaultNote's existing JWT access token and refresh-token cookie, keep
   access tokens out of redirect URLs, and obtain the frontend access token
   through the existing refresh flow.

### OAuth frontend

- Add provider buttons to the login page.
- Add a callback route that completes authentication through the existing
  refresh flow.
- Keep the access token only in frontend memory. Phone-based authentication
  remains outside the current scope.

### OAuth verification

- Add provider-mocked integration coverage and a local manual verification
  flow.

## Phase 8 — Notes

### Notes backend

- `USER` can read, create, update, and permanently delete only owned notes.
- `ADMIN` can read paginated lists of all notes, but cannot alter users, roles,
  or others' notes.
- Backend service-level ownership and role checks are mandatory. Angular guards
  and hidden UI controls are user experience only.
- Notes are private. Sharing is outside this iteration.

Build CRUD for notes with `BIGINT` IDs, title limited to 200 characters, and
Markdown content limited to 20,000 characters. Store source Markdown only;
the current API returns that source Markdown directly. Attachments and images
are outside this iteration.

Use `CurrentUserProvider` in note services for current-user ownership checks.
All collection endpoints are paginated. Sort a user's notes by `updatedAt`
descending by default. Do not add search yet. Use optimistic locking and return
`409 Conflict` as `ProblemDetail` if a note is updated from a stale version.

The implemented notes baseline currently includes the owner-only REST CRUD
endpoints, explicit request/response DTOs, pagination with `updatedAt DESC` as
the default order, `CurrentUserProvider` ownership checks, and centralized
`NOTE_NOT_FOUND` handling, optimistic locking through `ETag`/`If-Match`, and
PostgreSQL-backed endpoint integration coverage. The remaining Notes work is the
administrator read-only API. Add PostgreSQL-backed coverage for administrator
authorization and ownership isolation.

### Notes frontend

Implement the paginated Notes list and create/edit/delete flows, the read-only
administrator Notes view, and Markdown preview/read rendering. Parse the raw
Markdown returned by the API, sanitize generated HTML before inserting it into
the DOM, and allow only safe link protocols such as `http` and `https`. Generate
the TypeScript client from OpenAPI and add focused Angular unit tests for Notes
flows. Never persist access tokens in local storage or session storage.

## Phase 9 — Remaining Angular frontend

Complete the remaining cross-cutting Angular work with lazy-loaded routes and
simple local styles, without a UI component library:

- authenticated and administrator route guards;
- standard `ProblemDetail` error presentation on top of the implemented auth
  transport baseline;
- focused frontend unit tests and production-build verification;
- browser e2e tests remain deferred.

Never persist access tokens in local storage or session storage.

Keep Java tests deterministic and behavior-focused. Use AssertJ for assertions,
Mockito only for direct collaborators, and a shared PostgreSQL Testcontainer for
the integration-test JVM. Reset test data deterministically; never depend on
test order or arbitrary sleeps.

## Phase 10 — Documentation and publishing

- [x] Maintain Antora component metadata, navigation, and the published
  documentation site.
- [ ] Record the remaining architecture decision records.

## Phase 11 — Final security and email hardening

### Security and email backend

- [ ] (`MEDIUM-3`) Evaluate and choose a shared atomic counter store
  (PostgreSQL or Redis), validate it with concurrency/CI tests, activate it,
  and add edge/WAF enforcement for coarse public IP flood protection.
- [ ] Verify deployment-sensitive transport, SMTP, Mailpit, Swagger,
  registration-enumeration, and dependency supply-chain controls when a target
  deployment exists.

- [ ] Add verification-email resend and invalidate older active verification
  tokens when a new one is issued.
- [ ] Add cleanup and audit handling for expired or invalidated tokens.
- [ ] Add authentication audit events for successful and failed login,
  refresh-token reuse, logout, and email-verification security events.
- [ ] Add cleanup for expired refresh tokens. Remove records only after
  `expires_at`; retain revoked but not-yet-expired records so reuse detection
  remains possible.
- [ ] Revisit outbox delivery, retry, and SMTP-failure semantics when reliable
  asynchronous email delivery becomes necessary.

### Verification-email frontend

- [ ] Add resend controls to the registration success state and the invalid or
  expired verification-link state. Preserve the email, show neutral feedback,
  disable repeated submissions, and display the `Retry-After` countdown without
  sending automatically on page load.

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
14. [x] Rate-limiting storage and deployment boundary (`015`); local policy is
    accepted and shared provider selection plus deployment implementation remain
    in Phase 11.

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

- `SUPER_ADMIN`, tenant-aware roles, and granular permissions.
- Note sharing through explicit per-note permissions.
- Note trash, restore, and search.
- Editing email, logout from all devices, and session management UI.
- Complete audit records for privileged data access.
- A dedicated least-privilege DB user for production-like setups.
- Browser e2e tests, application CI, production deployment, TLS, and real SMTP.

## Final priority — Remaining hardening

The local application-level part of MEDIUM-3 is complete for the current
authentication flows. The remaining security, authentication, and email work is
tracked as the canonical checklist in Phase 11 — Final security and email
hardening.

## Future direction — separate React frontend

The repository may later contain a second, independent React SPA alongside the
existing Angular application. This is a multi-client setup, not a microfrontend
architecture:

- keep the current Angular SPA in `frontend/`;
- add the React SPA in a separate directory such as `frontend-react/`;
- keep one Spring Boot backend and one OpenAPI contract as the source of truth;
- give each client its own routing, UI components, in-memory access-token state,
  and HTTP/auth integration;
- reuse the `HttpOnly` refresh-cookie and CSRF protocol rather than sharing
  access tokens through browser storage;
- add both local origins to the backend CORS allowlist;
- implement the React client only after the Angular authentication and password
  recovery flows are stable.

The two SPAs should be independently runnable and deployable. They should not
be combined into one runtime shell unless a future product requirement makes
microfrontend composition necessary.
