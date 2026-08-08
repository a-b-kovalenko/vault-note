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

## Project boundaries

- This is a monorepo for an Angular SPA in `frontend/`, a Spring Boot backend
  in `backend/`, and Antora documentation in `docs/`.
- The backend will be a Java 25 Gradle multi-module build: `common`, `users`,
  `notes`, `security`, and runnable `app`. Keep dependencies directed toward
  stable lower-level modules; do not put domain-specific code in `common`.
- OpenAPI is the API source of truth. Do not expose JPA entities at REST
  boundaries; use explicit DTOs and RFC 9457 `ProblemDetail` errors.
- PostgreSQL uses database `vault_note` and schema `vaultnote`. Docker is for
  Mailpit only. Schema changes are forward-only Liquibase migrations; Hibernate
  validates rather than creates or evolves schemas.

## Security and data

- Notes are private to their owner. `ADMIN` may read paginated users and notes,
  but must not alter users, roles, or other users' notes.
- Keep the 15-minute access JWT only in Angular memory. Refresh tokens are
  rotating, hashed server-side, and delivered only by `HttpOnly` cookie; never
  log or persist credentials, JWTs, refresh tokens, or complete request
  payloads.
- Enforce authorization in backend services. Frontend guards and hidden UI are
  UX measures, not security controls.

## Verification

- When the backend quality gate exists, run `./gradlew check` from `backend/`.
- For a frontend change, run its focused unit tests and production build once
  the Angular workspace exists.
- Report every verification command run, its result, and anything not run.
