# VaultNote progress

`project-plan.md` records the detailed scope and architectural decisions.
This file is the live execution board: mark an item complete after its change
has been implemented and verified. Branch integration is tracked separately.

## Current focus

- Antora metadata, navigation, Gradle integration, and the GitHub Pages
  workflow are implemented; the site is published.
- The Mailpit baseline and the complete email-verification vertical slice are
  implemented and covered by PostgreSQL integration tests.
- Login endpoint, short-lived JWT issuance, refresh-token persistence,
  `HttpOnly` cookie delivery, refresh rotation, and reuse detection are
  implemented and verified.
- Bearer JWT validation and the protected `GET /api/v1/users/me` profile
  endpoint are implemented and covered by PostgreSQL integration tests.
- JWT `roles` claims are validated against the `UserRole` enum and mapped to
  Spring Security authorities; missing or unknown roles are rejected before the
  controller.
- The authorization slice now includes method security, the
  `CurrentUserProvider`, the admin user list, and OpenAPI bearer documentation.
- Unit and PostgreSQL integration coverage for login and refresh rotation is
  implemented and verified.
- Current-session logout and refresh-cookie clearing are implemented and
  verified.
- CORS and SPA-compatible CSRF protection are implemented and verified.
- The Angular authentication slice is implemented: generated auth models,
  `/login`, `/register`, `/verify-email`, in-memory access-token state, CSRF
  bootstrap, bearer attachment, single-flight refresh, `/me`, logout, password
  visibility toggles, `/forgot-password`, `/reset-password`, and focused unit
  tests.
- The local browser flows were verified end to end: registration sends a
  display-name-personalized verification email, `/verify-email` confirms it,
  login redirects to `/me`, profile data is loaded, and logout revokes the
  refresh session and returns to `/login`.
- The backend password-recovery baseline is implemented for local accounts:
  generic reset requests, hashed expiring one-time tokens, Mailpit links,
  password confirmation, email verification for unverified accounts, refresh
  session revocation, and stable invalid-token errors.
- The first three security-audit remediations (`HIGH-1`, `MEDIUM-1`, and
  `MEDIUM-2`) are implemented: JWT signing secrets are mandatory, known
  development placeholders are rejected, refresh-token family revocation
  commits before a reuse error is returned, and refresh/logout use the
  configured refresh-cookie name.
- The private Notes CRUD baseline is implemented: owner-only endpoints,
  pagination, DTO boundaries, `CurrentUserProvider` ownership checks,
  `NOTE_NOT_FOUND` handling, and PostgreSQL-backed integration coverage.
- The Notes API currently returns source Markdown; safe HTML rendering is
  planned as the final step of Phase 8, when a preview or read-only mode is
  introduced.
- `MEDIUM-3` is in progress: login, registration, and password-reset requests
  now have bounded in-memory IP- and normalized-email-aware limits with
  neutral `429` responses. A shared Angular rate-limit notice/countdown is
  integrated into the login, registration, and forgot-password screens; it
  reads the exposed `Retry-After` header, preserves form values, and disables
  submit while the limit is active. The check is performed in application
  services, including before password-reset lookup, token creation, and email
  delivery. Shared storage and edge/WAF hardening for multi-instance
  deployment remain; the shared-store provider is still open between
  PostgreSQL and Redis.
  Therefore, `MEDIUM-3` is only partially resolved: the local single-instance
  scope is complete, while the full finding closes after the remaining Phase 11
  tasks are completed.
  After that continue with OAuth, then the dedicated Notes phase, and finally
  the remaining cross-cutting Angular work such as route guards and standard
  frontend API-error presentation.
- The authenticated Angular application shell is implemented and verified: `/me`
  is rendered inside the shell, the account menu shows initials, display name,
  and email, the `ADMIN` link to All users is conditional, and logout is a
  separated action.
  Profile state is shared between the shell and child pages to avoid duplicate
  profile requests. The Profile screen now supports display-name save/cancel
  states while email and verification state remain read-only. Avatar upload,
  replacement, retrieval, removal, initials fallback, and generated OpenAPI
  client updates are implemented and verified. Avatar actions are available
  only in `Edit profile` mode. The read-only All users screen is also
  implemented and verified on top of the existing paginated API; it lists all
  registered users, keeps backend `ADMIN` authorization as the source of truth,
  and exposes no user-management actions.

## Phase 0 — Foundation

- [x] Establish the monorepo layout.
- [x] Create the backend Gradle scaffold with package-level feature boundaries.
- [x] Configure Java 25, Spotless, JaCoCo, and the aggregated quality gate.
- [x] Bootstrap the Spring Boot application.
- [x] Record the initial monorepo and Antora ADRs.
- [x] Add and verify Spring Boot Actuator.
- [x] Add a Bruno collection for the local health check.

## Phase 1 — Local platform and persistence

- [x] Add PostgreSQL dependencies and baseline local configuration.
- [x] Create the Liquibase master changelog and `vaultnote` schema baseline.
- [x] Add local and Testcontainers-driven test profiles.
  - [x] Add PostgreSQL Testcontainers, DBRider datasets, and HTTP integration
    coverage for the health, registration, and email-verification flows.
  - [x] Add the local test profile.
- [x] Add Mailpit Compose setup and non-secret environment example.

## Phase 2 — Identity and security

- [x] Model users, roles, and user-role assignments.
  - [x] Add `UserEntity` and `UserJpaRepository` for the users table.
  - [x] Add fixed numeric `UserRole` codes and persist assignments in
    `user_roles`.
  - [x] Assign the `USER` role to every newly registered user.
- [x] Implement registration and email verification.
  - [x] Add the registration endpoint and verify successful and duplicate-email
    flows against PostgreSQL.
  - [x] Add complete request validation: non-blank email/display name,
    email format, and the planned password policy.
    - [x] Return `VALIDATION_FAILED` with field-level `violations` and stable
      API error codes.
  - [x] Replace the temporary BCrypt encoder with Argon2id before registration
    reaches a production-like baseline.
  - [x] Implement the email-verification MVP after the Mailpit baseline.
    - [x] Add `email_verification_tokens` with a user relation, hashed token,
      expiry, `used_at`, and `created_at`.
    - [x] Generate a single-use token and send its raw value through
      `MailSender`.
    - [x] Add a dedicated email-verification endpoint.
    - [x] Atomically mark the token as used and set `email_verified` to `true`.
    - [x] Cover valid, invalid, expired, reused, and concurrent token scenarios
      against PostgreSQL.
- [x] Implement login, short-lived access JWTs, and rotating refresh sessions.
  - [x] Implement login, short-lived access JWT issuance, and initial refresh
    token cookie delivery.
  - [x] Validate bearer JWTs on protected requests and expose the authenticated
    user profile resource.
  - [x] Add PostgreSQL integration coverage for successful and failed login,
    including access-token response, refresh cookie, and persisted token hash.
  - [x] Implement refresh endpoint, rotation, and token-family reuse detection.
  - [x] Cover rotation, unknown, expired, and reused token scenarios with unit
    tests.
  - [x] Cover successful refresh rotation against PostgreSQL.
  - [x] Add the Bruno refresh request and verify the cookie-based flow against
    the local PostgreSQL database.
  - [x] Implement current-session logout and clear the refresh-token cookie.
  - [x] Cover logout, token revocation, cookie clearing, and missing-cookie
    behavior against PostgreSQL.
- [x] Add CORS and SPA-compatible CSRF protection.
  - [x] Configure an explicit Angular-origin allowlist with credentials.
  - [x] Expose `/csrf` with the `XSRF-TOKEN` cookie and
    `X-XSRF-TOKEN` header contract.
  - [x] Require the token for login, refresh, and logout requests.
  - [x] Verify token issuance, rejection, CORS preflight, and unknown-origin
    behavior with PostgreSQL-backed integration tests.
- [x] Add the current authorization baseline.
  - [x] Validate the JWT `roles` claim against `UserRole` and map it to Spring
    Security authorities.
  - [x] Enable method security and add `@PreAuthorize` rules for protected
    operations.
  - [x] Add `CurrentUserProvider` over `SecurityContextHolder`.
  - [x] Register the OpenAPI `bearerAuth` scheme and annotate protected
    endpoints with `@SecurityRequirement`.

## Phase 3 — Minimal Angular

- [x] Create the standalone Angular workspace and generated auth client.
- [x] Implement the email/password login page.
- [x] Add the minimum auth state for in-memory access-token storage, refresh,
  and CSRF handling.
- [x] Add focused frontend unit tests for the minimal login flow.
- [x] Add the authenticated `/me` screen and redirect after successful login.
- [x] Add current-session logout with local state clearing and login redirect.
- [x] Add password visibility toggle and accessible button state.
- [x] Verify the local browser flow: login → `/me` → logout → `/login`.
- [x] Implement the public registration page with client-side validation,
  password confirmation, and password visibility toggles.
- [x] Implement the `/verify-email` route with loading, success, and
  invalid-link states.
- [x] Verify the local browser flow: registration → email verification →
  login.

## Phase 4 — Password management prerequisite

### Password recovery backend

- [x] Add the backend baseline for unauthenticated local-account password
  recovery through email.
  - [x] Return a generic `202 Accepted` request response that prevents account
    enumeration.
  - [x] Store only a hash of a cryptographically random, one-time reset token
    with expiry and used-at metadata.
  - [x] Send the raw token only through the recovery email link and never log
    it.
  - [x] Confirm the reset with the existing password policy and
    `PasswordEncoder`; a successful reset also verifies an unverified local
    account.
  - [x] Atomically consume the token and revoke all active refresh sessions
    after a successful reset.
  - [x] Add stable `PASSWORD_RESET_FAILED` handling for invalid or expired
    tokens.
  - [x] Add service unit coverage for generic requests, expiry, password
    replacement, email verification, and session revocation.
  - [x] Add PostgreSQL endpoint coverage for generic responses, token reuse,
    session revocation, and concurrent one-time use.

### Password recovery frontend

- [x] Add the Angular forgot/reset-password screens, fresh-request path, and
  token removal from the address bar after success.

## Phase 5 — Security audit remediation

### Security remediation backend

- [x] (`HIGH-1`) Remove the known JWT fallback, require an explicit secret, and
  add stable local profile secret loading for Gradle and IntelliJ.
- [x] (`MEDIUM-1`) Make refresh-token family revocation commit independently
  when reuse detection returns an authentication error; add PostgreSQL
  committed-state coverage.
- [x] (`MEDIUM-2`) Use the configured refresh-cookie name consistently in login,
  refresh, logout, and cookie clearing; add extractor unit coverage, startup
  validation, and non-default cookie-name integration coverage.
- [x] (`MEDIUM-3`, local scope) Implement rate limiting for login, registration,
  and password reset with `429`/`Retry-After`, without account lockout or
  enumeration.
  - [x] Add IP and normalized-email limits for login with bounded local storage,
    early rejection, and endpoint integration coverage.
  - [x] Add IP and normalized-email limits for registration with bounded local
    storage, early rejection, and endpoint integration coverage.
  - [x] Add IP and normalized-email limits for password-reset requests with
    bounded local storage, generic responses, early rejection, and endpoint
    integration coverage.
  - [x] Document that multi-instance deployment requires shared atomic storage;
    the provider remains open between PostgreSQL and Redis pending
    concurrency/CI validation.

### Security remediation frontend

- [x] Add one shared Angular rate-limit notice/countdown to login, registration,
  and forgot-password screens; preserve form values, disable submit during the
  countdown, expose `Retry-After` through CORS, and cover the behavior with
  frontend tests.

## Phase 6 — Profile and user administration

### Profile backend

- [x] Implement the profile resource with read-only email and verification state
  plus editable `display_name` through `GET` and `PATCH /api/v1/users/me`.
  Share the display-name length rule between DTOs and the service, and cover
  the profile behavior with unit and PostgreSQL integration tests.
- [x] Add authenticated avatar upload, replacement, retrieval, and removal.
  Validate and normalize image content on the server and keep normalized
  256×256 JPEG bytes in separate avatar storage.
- [x] Implement the backend current-session logout endpoint.
- [x] Add the paginated user list API for administrators.

### Profile frontend

- [x] Migrate Angular `/me` to `GET /api/v1/users/me`, rename the API method
  from `currentUser()` to `profile()`, regenerate the OpenAPI client, and
  prepare `updateProfile()` for the future editable profile screen.
- [x] Add the authenticated application shell and account menu with Profile,
  conditional All users access for administrators, and a separated Log out
  action.
- [x] Implement the Profile screen with read-only email and verification state,
  editable `displayName`, and save/cancel states.
- [x] Add avatar preview, upload, replacement, removal, and initials fallback.
  Share avatar state between the authenticated shell and Profile, show avatar
  actions only in `Edit profile` mode, and regenerate the OpenAPI client after
  the backend contract became available.
- [x] Add the read-only All users screen for administrators using the existing
  paginated API.

## Phase 7 — OAuth2/OIDC sign-in

### OAuth backend

- [ ] Complete authenticated password management for provider/passwordless
  accounts.
  - [ ] Make `users.password_hash` nullable.
  - [ ] Persist provider identities with uniqueness constraints.
- [ ] Implement authenticated password management.
  - [ ] Add authenticated set/change-password use cases and endpoint.
  - [ ] Require and verify `currentPassword` when changing an existing password.
  - [ ] Allow an authenticated provider user to set a password when none exists.
  - [ ] Reuse the existing password policy and `PasswordEncoder`.
- [ ] Prevent removing the last available authentication method.
  - [ ] Add unit and PostgreSQL integration coverage for set, change, and
    invariant cases.
- [ ] Add backend OAuth2 Client integration.
  - [ ] Configure a provider such as Google or GitHub.
  - [ ] Use a short-lived cookie-based authorization state compatible with the
    stateless application security model.
  - [ ] Implement authorization redirect and callback handling.
  - [ ] Map verified provider identities to local users and assign `USER` by
    default.
  - [ ] Handle explicit onboarding and account linking; never link by email
    match alone.
  - [ ] Issue the existing JWT access token and refresh-token cookie.
  - [ ] Never place access or refresh tokens in redirect URLs.

### OAuth frontend

- [ ] Add the Angular OAuth flow.
  - [ ] Add provider buttons to the login page.
  - [ ] Add a callback route that completes authentication through refresh.
  - [ ] Keep the access token only in frontend memory.

### OAuth verification

- [ ] Add provider-mocked integration coverage and a local manual verification
  flow.

## Phase 8 — Notes

### Notes backend

- [x] Implement private notes CRUD, ownership checks, and pagination.
  - [x] Use `CurrentUserProvider` in note services for current-user ownership
    checks.
  - [x] Add PostgreSQL-backed integration coverage for CRUD, pagination,
    ownership isolation, validation, and authentication.
  - [x] Add optimistic locking through `ETag`/`If-Match`.
    - [x] Return `409 Conflict` for stale note updates using `ETag`/`If-Match`
      and verify the stale-update flow against PostgreSQL.
- [ ] Add the read-only administrator Notes API with role checks and PostgreSQL
  authorization/ownership coverage.

### Notes frontend

- [ ] Implement the paginated Notes list and create/edit/delete screens.
- [ ] Add the read-only administrator Notes view.
- [ ] Sanitize rendered Markdown and enforce the safe Markdown subset.
- [ ] As the final Notes UI task, add Markdown preview/read rendering.
  - [ ] Parse the raw Markdown returned by the API.
  - [ ] Sanitize generated HTML before inserting it into the DOM.
  - [ ] Allow only safe link protocols such as `http` and `https`.

## Phase 9 — Remaining Angular frontend

- [ ] Add authenticated and administrator route guards.
- [ ] Add standard `ProblemDetail` error presentation.
- [ ] Add focused frontend unit tests and production-build verification.
- [ ] Defer browser e2e tests.

## Phase 10 — Documentation and publishing

- [x] Add Antora component metadata and navigation.
- [ ] Record the remaining architecture decision records.
  - [x] Record the fixed numeric role-code strategy (`009`).
  - [x] Record JWT access and initial refresh-token delivery (`010`).
  - [x] Record refresh-token rotation and reuse detection (`011`).
  - [x] Record current-session logout (`012`).
  - [x] Record the CSRF and CORS strategy (`013`).
  - [x] Record the rate-limiting storage and deployment boundary (`015`).
- [x] Complete README setup, operations, and verification instructions.
- [x] Publish Antora documentation to GitHub Pages.

## Phase 11 — Final security and email hardening

### Security and email backend

- [ ] (`MEDIUM-3`) Evaluate and choose a shared atomic counter store
  (PostgreSQL or Redis), validate it with concurrency/CI tests, activate it,
  and add edge/WAF enforcement for coarse public IP flood protection.
- [ ] Verify deployment-sensitive transport, SMTP, Mailpit, Swagger,
  registration-enumeration, and dependency supply-chain controls when a target
  deployment exists.

- [ ] Add verification-email resend.
- [ ] Invalidate older active verification tokens when a new one is issued.
- [ ] Add cleanup and audit handling for expired or invalidated tokens.
- [ ] Add authentication audit events for successful and failed login,
  refresh-token reuse, logout, and email-verification security events.
- [ ] Add cleanup for expired refresh tokens.
  - [ ] Remove records only after `expires_at`.
  - [ ] Retain revoked but not-yet-expired records so reuse detection remains
    possible.
- [ ] Revisit outbox delivery, retry, and SMTP-failure semantics when reliable
  asynchronous email delivery becomes necessary.

### Verification-email frontend

- [ ] Add resend controls to the registration success state and the invalid or
  expired verification-link state. Preserve the email, show neutral feedback,
  disable repeated submissions, and display the `Retry-After` countdown without
  sending automatically on page load.

## Final priority — Remaining hardening

The local application-level part of MEDIUM-3 is complete for the current
authentication flows. The shared-store choice (PostgreSQL or Redis) and the
remaining security, authentication, and email work are tracked as the canonical
checklist in Phase 11 — Final security and email hardening.

## Planned separate frontend client

- [ ] Add an independent React SPA alongside the existing Angular SPA.
  - [ ] Keep Angular in `frontend/` and add React in a separate directory such
    as `frontend-react/`.
  - [ ] Reuse the Spring Boot API and OpenAPI contract without duplicating
    backend authentication logic.
  - [ ] Keep access tokens in each client's memory and reuse the existing
    `HttpOnly` refresh-cookie and CSRF protocol.
  - [ ] Add both frontend origins to the backend CORS allowlist.
  - [ ] Verify equivalent login, registration, email verification, password
    recovery, refresh, `/me`, and logout flows.
  - [ ] Keep the clients as separate SPAs; do not introduce microfrontend
    composition without a concrete product requirement.
