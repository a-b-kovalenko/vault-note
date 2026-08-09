# VaultNote progress

`project-plan.md` records the detailed scope and architectural decisions.
This file is the live execution board: mark an item complete only after its
change has been merged into `main`.

## Current focus

- Antora metadata, navigation, Gradle integration, and the GitHub Pages
  workflow are implemented; the site is published.
- The Mailpit baseline and the first part of the email-verification vertical
  slice are implemented.
- Next: add the dedicated email-verification endpoint and atomically consume
  the token.
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
    coverage for the health and registration flows.
  - [x] Add the local test profile.
- [x] Add Mailpit Compose setup and non-secret environment example.

## Phase 2 — Identity and security

- [ ] Model users, roles, and user-role assignments.
  - [x] Add `UserEntity` and `UserJpaRepository` for the users table.
- [ ] Implement registration and email verification.
  - [x] Add the registration endpoint and verify successful and duplicate-email
    flows against PostgreSQL.
  - [x] Add complete request validation: non-blank email/display name,
    email format, and the planned password policy.
    - [x] Return `VALIDATION_FAILED` with field-level `violations` and stable
      API error codes.
  - [x] Replace the temporary BCrypt encoder with Argon2id before registration
    reaches a production-like baseline.
  - [ ] Implement the email-verification MVP after the Mailpit baseline.
    - [x] Add `email_verification_tokens` with a user relation, hashed token,
      expiry, `used_at`, and `created_at`.
    - [x] Generate a single-use token and send its raw value through
      `MailSender`.
    - [ ] Add a dedicated email-verification endpoint.
    - [ ] Atomically mark the token as used and set `email_verified` to `true`.
- [ ] Implement login, short-lived access JWTs, and rotating refresh sessions.
- [ ] Add CSRF, CORS, rate limiting, and refresh-token reuse handling.
  - [ ] Start CSRF work after the registration vertical slice is implemented
    and verified.
  - [ ] Enable Spring Security SPA CSRF, expose `/csrf`, and require the token
    for login, refresh, and logout requests.
- [ ] Add authentication audit events and authorization rules.

## Phase 3 — Notes and profile

- [ ] Implement private notes CRUD, ownership checks, and pagination.
- [ ] Add optimistic locking and safe Markdown rendering.
- [ ] Implement profile display-name updates and current-session logout.
- [ ] Add read-only administrator user and note views.

## Phase 4 — Angular frontend

- [ ] Create the standalone Angular workspace and generated OpenAPI client.
- [ ] Implement authentication, refresh, and route-guard flows.
- [ ] Implement notes, profile, and administrator screens.
- [ ] Add focused frontend unit tests and production-build verification.

## Phase 5 — Documentation and publishing

- [x] Add Antora component metadata and navigation.
- [ ] Record the remaining architecture decision records.
- [x] Complete README setup, operations, and verification instructions.
- [x] Publish Antora documentation to GitHub Pages.

## Phase 6 — Email and authentication hardening

- [ ] Add rate-limited verification-email resend.
- [ ] Invalidate older active verification tokens when a new one is issued.
- [ ] Add cleanup and audit handling for expired or invalidated tokens.
- [ ] Revisit outbox delivery, retry, and SMTP-failure semantics when reliable
  asynchronous email delivery becomes necessary.
