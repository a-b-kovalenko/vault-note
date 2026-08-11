# VaultNote

VaultNote is a local learning project for private Markdown notes. It combines
an Angular single-page application, a Spring Boot API, PostgreSQL, and a
production-inspired authentication lifecycle.

> **Project status:** the Angular workspace is ready for login-page work. The
> repository contains registration, email verification, login, JWT access
> tokens, rotating refresh sessions, current-session logout, users and Notes
> APIs, optimistic locking, local/test profiles, CORS, SPA-compatible CSRF
> protection, PostgreSQL integration coverage, and an Angular workspace. Rate
> limiting, audit events, and the frontend login flow are still planned.

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

## Delivery phases

- Phase 4: minimal Angular workspace with the email/password login page.
- Phase 5: OAuth2/OIDC sign-in with provider buttons and a callback flow.
- Phase 6: remaining Angular screens, route guards, refresh handling, and
  frontend Markdown preview/read rendering.
- Later phases: documentation maintenance and final email/security hardening.

## Engineering conventions

The repository follows the shared personal engineering standards. Agent
instructions are in [.agents/AGENTS.md](.agents/AGENTS.md). The backend quality
gate is run with:

```shell
cd backend
./gradlew check
```

This runs formatting, unit tests, PostgreSQL/Testcontainers integration tests,
Liquibase validation, the Angular frontend tests and production build, and an
80% JaCoCo coverage gate. Docker is required for the Testcontainers integration
tests.

## Local development

The local profile expects a PostgreSQL database named `vault_note`. The
application uses the `vaultnote` schema and applies its schema through
Liquibase. By default it connects to `localhost:5432` as `user` with password
`password`; override these values with `VAULTNOTE_DATABASE_URL`,
`VAULTNOTE_DATABASE_USERNAME`, and `VAULTNOTE_DATABASE_PASSWORD` when needed.
The browser CORS allowlist defaults to `http://localhost:4200`; override it
with `VAULTNOTE_CORS_ALLOWED_ORIGINS` when the frontend uses another origin.

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
network access to download Node.js and install the locked Antora and Angular
dependencies. The generated site is written to `docs/build/site` and the
frontend production bundle to `frontend/dist/frontend`. The regular `check` task
includes both documentation and frontend verification.

Run only the frontend checks when working on Angular:

```shell
cd backend
./gradlew frontendTest frontendBuild
```

## Implemented authentication API

The backend currently supports registration, email verification, login,
refresh-token rotation, current-session logout, and browser CSRF/CORS
protection. Before a state-changing authentication request, obtain a CSRF
token and preserve its cookie:

```shell
curl --include --cookie-jar cookies.txt http://localhost:8080/csrf
```

Copy the `token` value from the response and send it in the
`X-XSRF-TOKEN` header together with the cookie jar. A verified user can log in
with:

```shell
curl --include --request POST http://localhost:8080/api/v1/auth/login \
  --cookie cookies.txt \
  --cookie-jar cookies.txt \
  --header 'Content-Type: application/json' \
  --header 'X-XSRF-TOKEN: <csrf-token>' \
  --data '{"email":"user@example.com","password":"Password1234"}'
```

The response contains a short-lived JWT access token and sets the raw refresh
token in the `HttpOnly` `vaultnote_refresh_token` cookie. Refresh the session by
sending that cookie back:

```shell
curl --include --request POST http://localhost:8080/api/v1/auth/refresh \
  --cookie cookies.txt \
  --cookie-jar cookies.txt \
  --header 'X-XSRF-TOKEN: <csrf-token>'
```

Each successful refresh revokes the old refresh token, creates a replacement in
the same token family, and returns a new cookie. Reusing an already rotated
token revokes the active family and returns `401`.

Logout the current refresh session with:

```shell
curl --include --request POST http://localhost:8080/api/v1/auth/logout \
  --cookie cookies.txt \
  --cookie-jar cookies.txt \
  --header 'X-XSRF-TOKEN: <csrf-token>'
```

The endpoint returns `204`, revokes the matching refresh token, and clears the
cookie with `Max-Age=0`. An absent or already invalid cookie is handled
idempotently. An issued access JWT remains valid until its expiration.

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

## Implemented notes API

The protected notes resource provides owner-only CRUD:

- `GET /api/v1/notes` — paginated current-user notes, sorted by `updated_at`
  descending by default;
- `GET /api/v1/notes/{noteId}` — get one owned note;
- `POST /api/v1/notes` — create a note and return `201`;
- `PUT /api/v1/notes/{noteId}` — update an owned note;
- `DELETE /api/v1/notes/{noteId}` — permanently delete an owned note and return
  `204`.

All requests require a bearer access token. `POST`, `PUT`, and `DELETE` also
require the CSRF cookie/header contract described above. Create and update
requests use:

```json
{
  "title": "My note",
  "content": "# Markdown content"
}
```

Titles are limited to 200 characters and content to 20,000 characters. The
service applies ownership through `CurrentUserProvider`; a missing or another user's
note returns `404` with code `NOTE_NOT_FOUND`.

Optimistic locking is implemented through `ETag`/`If-Match`. The API currently
returns source Markdown; safe HTML rendering is deferred until a preview or
read-only mode is introduced at the end of the Angular phase. Administrator
note views and broader
PostgreSQL-backed notes integration coverage remain planned.

## Planning and progress

The published [VaultNote documentation](https://a-b-kovalenko.github.io/vault-note/)
contains the Antora overview and architecture decisions.

The tracked [implementation plan](docs/project-plan.md) and
[execution board](docs/project-progress.md) describe the intended build-out
and current delivery status. The numbered [Architecture Decision
Records](docs/modules/ROOT/pages/adr/) capture the decisions that are already
implemented or superseded. Temporary handoffs and working notes remain in
`.workspace/` and are excluded from version control.
