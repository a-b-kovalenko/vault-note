# VaultNote

VaultNote is a local learning project for private Markdown notes. It combines
an Angular single-page application, a Spring Boot API, PostgreSQL, and a
production-inspired authentication lifecycle.

> **Project status:** backend foundation in progress. The repository currently
> contains the users persistence baseline, registration endpoint, request
> validation, Argon2id password hashing, local/test profiles, and PostgreSQL
> integration coverage. Email verification, the remaining authentication flows,
> and the frontend are still planned.

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

## Target product scope

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
instructions are in [.agents/AGENTS.md](.agents/AGENTS.md). The backend quality
gate is run with:

```shell
cd backend
./gradlew check
```

This runs formatting, unit tests, PostgreSQL/Testcontainers integration tests,
Liquibase validation, and an 80% JaCoCo coverage gate. Docker is required for
the Testcontainers integration tests.

## Local development

The local profile expects a PostgreSQL database named `vault_note`. The
application uses the `vaultnote` schema and applies its schema through
Liquibase. By default it connects to `localhost:5432` as `user` with password
`password`; override these values with `VAULTNOTE_DATABASE_URL`,
`VAULTNOTE_DATABASE_USERNAME`, and `VAULTNOTE_DATABASE_PASSWORD` when needed.

Start the backend with the `local` profile after the shared PostgreSQL instance
is available:

```shell
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Build the Antora documentation site locally:

```shell
cd backend
./gradlew antoraBuild
```

The documentation build requires Node.js 20 or newer and npm. The generated
site is written to `docs/build/site`. The regular `check` task also includes
the documentation build.

## Implemented registration API

Register a user with `POST /api/v1/auth/registrations`:

```shell
curl --request POST http://localhost:8080/api/v1/auth/registrations \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "user@example.com",
    "display_name": "VaultNote User",
    "password": "Password1234"
  }'
```

The request requires a non-blank, valid email, a display name up to 100
characters, and a password from 12 to 256 characters containing at least two
digits and one alphabetic character. A successful request returns `201` with
the new user identifier; the password is stored only as an Argon2id hash and
the account starts unverified.

Invalid requests return `400` with code `VALIDATION_FAILED` and field-level
`violations`. A duplicate email returns `409` with code
`ENTITY_ALREADY_EXISTS`.

## Planning and progress

The tracked [implementation plan](docs/project-plan.md) and
[execution board](docs/project-progress.md) describe the intended build-out
and current delivery status. The numbered [Architecture Decision
Records](docs/modules/ROOT/pages/adr/) capture the decisions that are already
implemented or superseded. Temporary handoffs and working notes remain in
`.workspace/` and are excluded from version control.
