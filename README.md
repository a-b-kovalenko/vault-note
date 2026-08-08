# VaultNote

VaultNote is a local learning project for private Markdown notes. It combines
an Angular single-page application, a Spring Boot API, PostgreSQL, and a
production-inspired authentication lifecycle.

> **Project status:** backend foundation in progress. The repository contains
> the initial registration slice, persistence baseline, and local quality gate;
> the frontend and remaining authentication flows are still planned.

## Planned architecture

```text
vault-note/
├── backend/      Spring Boot, Java 25, single Gradle project
├── frontend/     Angular standalone application
└── docs/         Antora documentation and architecture decision records
```

The backend uses package-level boundaries for `common`, `users`, `notes`,
`security`, and the runnable `app` package. OpenAPI will define the API
contract and generate the frontend client.

## Product scope

- Private Markdown notes with owner-only CRUD, pagination, and optimistic
  locking.
- Email/password registration with email verification.
- A 15-minute JWT access token held only in Angular memory and a rotating,
  7-day `HttpOnly` refresh-token cookie.
- `USER` and read-only `ADMIN` roles.
- PostgreSQL database `vault_note`, using the non-public `vaultnote` schema.
- Mailpit for local email testing; PostgreSQL remains an existing local server.

Raw HTML, attachments, note sharing, password reset, OAuth2, search, browser
e2e tests, application CI, and production deployment are outside the first
iteration.

## Engineering conventions

The repository follows the shared personal engineering standards. Agent
instructions are in [.agents/AGENTS.md](.agents/AGENTS.md). The eventual
backend quality gate will be run with:

```shell
cd backend
./gradlew check
```

It will include formatting, unit tests, PostgreSQL/Testcontainers integration
tests, Liquibase validation, and an 80% JaCoCo coverage gate.

## Planning and progress

The tracked [implementation plan](docs/project-plan.md) and
[execution board](docs/project-progress.md) describe the intended build-out
and current delivery status. Temporary handoffs and working notes remain in
`.workspace/` and are excluded from version control.
