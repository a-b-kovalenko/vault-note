# VaultNote agent guidance

Before making any change, read the canonical personal working rules:
`/Users/akovalenko/Projects/Personal/engineering-standards/agent/WORKING_RULES.md`.

Before editing Java, read:
`/Users/akovalenko/Projects/Personal/engineering-standards/java/AGENT_RULES.md`.
Read the referenced testing, quality-gate, and formatting guidance when the
change involves those concerns.

## Collaboration

- Work in small, reviewable changes. Before each change, explain its purpose,
  affected files, risks, and verification; wait for the user to choose whether
  to write it, request a scaffold, or ask for implementation.
- Keep the canonical implementation plan in `docs/project-plan.md` and the
  execution board in `docs/project-progress.md`; both are tracked in Git.
  Update progress when the user explicitly requests it after the relevant
  scope has been implemented and verified; do not wait for a merge into
  `main`.
- Store only temporary plans, handoffs, and working notes in `.workspace/`.
- Preserve unrelated user changes, including local IDE configuration.
- Never create a Git commit or push changes without the user's explicit command
  for that action.

## IDE problem checks

- When the user asks to check IDE problems, issues, warnings, errors, or uses
  an equivalent Ukrainian or English phrase such as “перевір проблеми”,
  “problems”, “issues”, or “що підсвічує IDE” without naming a file, use
  IntelliJ IDEA MCP to inspect problems in the currently open file.
- When the user provides a file path, check whether it is open; open it in
  IntelliJ IDEA first when necessary, then inspect its problems through the IDE
  MCP.
- Fix reported problems when the change is appropriate and safe. Use a focused
  verification after the fix when needed.
- Do not run a full IntelliJ IDEA build for these checks.

## RTK shell output

RTK (Rust Token Killer) is a CLI proxy that compresses noisy shell output to
reduce context usage by the AI agent.

- RTK is configured globally for Codex through `rtk init -g --codex`.
- Prefix shell commands with `rtk` to receive compact output and preserve
  context, for example `rtk git status` or `rtk ./gradlew test`.
- Use `rtk git diff --no-compact` when the complete diff is needed.
- Use `rtk proxy <command>` when the original unfiltered output is required,
  especially for detailed diagnostics.

## Java package structure

- Organize packages by architectural role and feature ownership.
- Keep API DTOs near their feature: `app.api.<feature>.dto`.
- Keep application configuration in `app.config`.
- Keep service contracts in `app.service` and implementations in
  `app.service.impl`.
- Keep application exceptions in `app.exception`.
- Separate persistence entities and repositories:
  `infrastructure.persistence.entity` and
  `infrastructure.persistence.repository`.
- Do not mix controllers, DTOs, entities, and repositories in one package.
- Avoid generic packages such as `model`, `data`, or `util` unless their
  responsibility is genuinely cross-cutting.

## Obsidian knowledge vault

Before editing `VaultNote Atlas/`, read its local instructions in
`VaultNote Atlas/AGENTS.md` and the shared Markdown and Obsidian standard:
`/Users/akovalenko/Projects/Personal/engineering-standards/obsidian/AGENT_RULES.md`.

When the user asks to create a note without naming another destination, treat
it as a Ukrainian learning note in `VaultNote Atlas/content/`. If the request
could refer either to the Atlas, public documentation, or an ADR, clarify the
destination before creating the file.

## Project boundaries

- This is a monorepo for an Angular SPA in `frontend/`, a Spring Boot backend
  in `backend/`, Antora documentation in `docs/`, and the Ukrainian Obsidian
  vault in `VaultNote Atlas/`.
- The backend is a Java 25 single-module Gradle build organized by package-level
  feature boundaries: `common`, `users`, `notes`, `security`, and runnable
  `app`. Keep dependencies directed toward stable lower-level packages; do not
  put domain-specific code in `common`.
- OpenAPI is the API source of truth. Do not expose JPA entities at REST
  boundaries; use explicit DTOs and RFC 9457 `ProblemDetail` errors.
- PostgreSQL uses database `vault_note` and schema `vaultnote`. Docker is for
  Mailpit only. Schema changes are forward-only Liquibase migrations; Hibernate
  validates rather than creates or evolves schemas.

## Delivery phases

- Phase 4 is intentionally minimal: create the Angular workspace and implement
  only the email/password login page plus the auth state needed for in-memory
  access tokens, refresh, and CSRF.
- Phase 5 adds OAuth2/OIDC sign-in. The backend owns the provider redirect and
  callback, maps a verified provider identity to a local user, and then issues
  VaultNote's existing JWT and refresh cookie. Never put either token in a URL.
- Phase 6 adds the remaining Angular screens, guards, refresh/interceptor
  behavior, and the final Markdown preview/read rendering. Until then, the API
  returns source Markdown and the frontend does not render it as HTML.

## Security and data

- When the user asks to find, inspect, or verify data in the VaultNote
  PostgreSQL database, use the project-scoped `vaultnote-postgres` MCP from
  `.codex/config.toml` first. Use read-only queries against database
  `vault_note` and schema `vaultnote`; do not use Docker, `psql`, or the IDEA
  database tools as the first path. If the MCP is unavailable, report that
  explicitly before using a fallback.
- Notes are private to their owner. `ADMIN` may read paginated users and notes,
  but must not alter users, roles, or other users' notes.
- Keep the 15-minute access JWT only in Angular memory. Refresh tokens are
  rotating, hashed server-side, and delivered only by `HttpOnly` cookie; never
  log or persist credentials, JWTs, refresh tokens, or complete request
  payloads.
- Enforce authorization in backend services. Frontend guards and hidden UI are
  UX measures, not security controls.
- OAuth2/OIDC identities must come from a verified provider response. Assign
  `USER` by default, define account-linking rules explicitly, and reuse the
  existing VaultNote token lifecycle after successful provider authentication.

## Verification

- When the backend quality gate exists, run `./gradlew check` from `backend/`.
- For a frontend change, run its focused unit tests and production build once
  the Angular workspace exists.
- Report every verification command run, its result, and anything not run.
