# VaultNote progress

`project-plan.md` records the detailed scope and architectural decisions.
This file is the live execution board: mark an item complete only after its
change has been merged into `main`.

## Current focus

- Next small change: add local and Testcontainers-driven test profiles.

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
- [ ] Add local and Testcontainers-driven test profiles.
- [ ] Add Mailpit Compose setup and non-secret environment example.

## Phase 2 — Identity and security

- [ ] Model users, roles, and user-role assignments.
  - [x] Add `UserEntity` and `UserJpaRepository` for the users table.
- [ ] Implement registration and email verification.
  - [ ] Add complete request validation: non-blank email/display name,
    email format, and the planned password policy.
  - [ ] Replace the temporary BCrypt encoder with Argon2id before registration
    reaches a production-like baseline.
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

- [ ] Add Antora component metadata and navigation.
- [ ] Record the remaining architecture decision records.
- [ ] Complete README setup, operations, and verification instructions.
- [ ] Publish Antora documentation to GitHub Pages.
