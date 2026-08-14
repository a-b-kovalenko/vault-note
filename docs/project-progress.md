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
- Bearer JWT validation and the protected `GET /api/v1/auth/me` endpoint are
  implemented and covered by PostgreSQL integration tests.
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
  login redirects to `/me`, current user data is loaded, and logout revokes the
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
  planned as the final step of the Angular phase, when a preview or read-only
  mode is introduced.
- `MEDIUM-3` is in progress: login and registration now have bounded local
  IP- and normalized-email-aware limits with neutral `429` responses. Password
  reset limits and the shared-storage or edge/WAF policy for multi-instance
  deployment remain. Deployment-sensitive hardening is also still required.
  After that continue with OAuth, the Notes administrator view, profile
  updates, authenticated route guards, and standard frontend API-error
  presentation.
- The local profile is implemented and verified locally.

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
  - [x] Validate bearer JWTs on protected requests and expose the current-user
    access-check endpoint.
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

## Phase 3 — Notes and profile

- [x] Implement private notes CRUD, ownership checks, and pagination.
  - [x] Use `CurrentUserProvider` in note services for current-user ownership
    checks.
  - [x] Add PostgreSQL-backed integration coverage for CRUD, pagination,
    ownership isolation, validation, and authentication.
  - [x] Add optimistic locking through `ETag`/`If-Match`.
    - [x] Return `409 Conflict` for stale note updates using `ETag`/`If-Match`
      and verify the stale-update flow against PostgreSQL.
  - [ ] Sanitize rendered Markdown and enforce the safe Markdown subset.
- [ ] Implement profile display-name updates.
- [x] Implement the backend current-session logout endpoint.
- [ ] Add read-only administrator user and note views.
  - [x] Add the paginated administrator user list.
  - [ ] Add the read-only administrator note view.

## Phase 4 — Minimal Angular

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

## Phase 4.5 — Password management prerequisite

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
- [x] Add the Angular forgot/reset-password screens, fresh-request path, and
  token removal from the address bar after success.

## Phase 4.75 — Security audit remediation

- [x] (`HIGH-1`) Remove the known JWT fallback, require an explicit secret, and
  add stable local profile secret loading for Gradle and IntelliJ.
- [x] (`MEDIUM-1`) Make refresh-token family revocation commit independently
  when reuse detection returns an authentication error; add PostgreSQL
  committed-state coverage.
- [x] (`MEDIUM-2`) Use the configured refresh-cookie name consistently in login,
  refresh, logout, and cookie clearing; add extractor unit coverage, startup
  validation, and non-default cookie-name integration coverage.
- [ ] (`MEDIUM-3`) Define and implement rate limiting for login, registration,
  and password reset with `429`/`Retry-After`, without account lockout or
  enumeration.
  - [x] Add IP and normalized-email limits for login with bounded local storage,
    early rejection, and endpoint integration coverage.
  - [x] Add IP and normalized-email limits for registration with bounded local
    storage, early rejection, and endpoint integration coverage.
  - [ ] Add limits for password reset.
  - [ ] Select shared atomic storage or edge/WAF enforcement for multi-instance
    deployments.
- [ ] Verify deployment-sensitive transport, SMTP, Mailpit, Swagger,
  registration-enumeration, and dependency supply-chain controls when a target
  deployment exists.

## Phase 5 — OAuth2/OIDC sign-in

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
- [ ] Add the Angular OAuth flow.
  - [ ] Add provider buttons to the login page.
  - [ ] Add a callback route that completes authentication through refresh.
  - [ ] Keep the access token only in frontend memory.
- [ ] Add provider-mocked integration coverage and a local manual verification
  flow.

## Phase 6 — Remaining Angular frontend

- [ ] Implement Notes, profile, and administrator screens.
- [ ] Add authenticated and administrator route guards.
- [ ] Add standard `ProblemDetail` error presentation.
- [ ] Add focused frontend unit tests and production-build verification.
- [ ] As the final frontend task, add Markdown preview/read rendering.
  - [ ] Parse the raw Markdown returned by the API.
  - [ ] Sanitize generated HTML before inserting it into the DOM.
  - [ ] Allow only safe link protocols such as `http` and `https`.
- [ ] Defer browser e2e tests.

## Phase 7 — Documentation and publishing

- [x] Add Antora component metadata and navigation.
- [ ] Record the remaining architecture decision records.
  - [x] Record the fixed numeric role-code strategy (`009`).
  - [x] Record JWT access and initial refresh-token delivery (`010`).
  - [x] Record refresh-token rotation and reuse detection (`011`).
  - [x] Record current-session logout (`012`).
  - [x] Record the CSRF and CORS strategy (`013`).
- [x] Complete README setup, operations, and verification instructions.
- [x] Publish Antora documentation to GitHub Pages.

## Phase 8 — Email and authentication hardening

- [ ] Add verification-email resend.
- [ ] Invalidate older active verification tokens when a new one is issued.
- [ ] Add cleanup and audit handling for expired or invalidated tokens.
- [ ] Revisit outbox delivery, retry, and SMTP-failure semantics when reliable
  asynchronous email delivery becomes necessary.

## Final priority — Remaining rate limiting

- [ ] Complete rate limiting for the remaining public authentication flows.
  - [x] Protect login with IP- and email-aware limits without locking user
    accounts.
  - [x] Protect registration with IP- and email-aware limits.
  - [ ] Protect password reset with IP- and email-aware limits.
  - [ ] Prefer Cloudflare or another edge/WAF rule for public traffic.
  - [ ] Add application-level limits only for direct-origin or internal
    traffic, or for email-specific rules the edge cannot enforce.
- [ ] Add authentication audit events for successful and failed login,
  refresh-token reuse, logout, and email-verification security events.
- [ ] Add cleanup for expired refresh tokens.
  - [ ] Remove records only after `expires_at`.
  - [ ] Retain revoked but not-yet-expired records so reuse detection remains
    possible.

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
