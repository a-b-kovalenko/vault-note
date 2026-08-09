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
├── backend/         Spring Boot, Java 25, single Gradle project
├── frontend/        Angular standalone application
├── docs/            Antora documentation and architecture decision records
└── VaultNote Atlas/ Ukrainian Obsidian learning vault
```

The backend uses package-level boundaries for `common`, `users`, `notes`,
`security`, and the runnable `app` package. OpenAPI will define the API
contract and generate the frontend client.

## Obsidian knowledge vault

`VaultNote Atlas/` is a Ukrainian-only Obsidian vault for explanations and
concepts that emerge during project work. Open this directory as the vault
root in Obsidian. It follows the navigation-first approach used by the
`java-kb` vault, adapted to this smaller project:

```text
VaultNote Atlas/
├── VaultNote_Atlas.md  central MOC and navigation
├── content/            all learning notes
└── assets/             shared images and diagrams
```

The workflow is: capture an open question as a draft in `content/`, refine the
useful explanation, add a short TL;DR and a link to the central MOC, then
register the note under the appropriate topic in `VaultNote_Atlas.md`. Stable
project decisions belong in the public Antora documentation and ADRs; the
vault keeps the Ukrainian learning context around them. The detailed local
rules are in [.agents/AGENTS.md](.agents/AGENTS.md).

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

Mailpit provides the local SMTP server and web UI for inspecting outgoing mail.
Copy the non-secret example environment file and start only the Mailpit
service:

```shell
cp .env.example .env
docker compose up -d mailpit
```

The backend sends SMTP traffic to `localhost:1025`; inspect captured messages at
[http://localhost:8025](http://localhost:8025). Override the Mailpit ports or
SMTP settings through `.env` when the defaults are already in use.

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

The Gradle build downloads the pinned Node.js runtime automatically, so a
system-wide Node.js or npm installation is not required. The first run needs
network access to download Node.js and install the locked Antora dependencies.
The generated site is written to `docs/build/site`. The regular `check` task
also includes the documentation build.

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

The published [VaultNote documentation](https://a-b-kovalenko.github.io/vault-note/)
contains the Antora overview and architecture decisions.

The tracked [implementation plan](docs/project-plan.md) and
[execution board](docs/project-progress.md) describe the intended build-out
and current delivery status. The numbered [Architecture Decision
Records](docs/modules/ROOT/pages/adr/) capture the decisions that are already
implemented or superseded. Temporary handoffs and working notes remain in
`.workspace/` and are excluded from version control.
