## 📝 TL;DR

Це центральна карта українського сховища VaultNote Atlas. Усі пояснення
зберігаються в `content/`, а тут групуються за темами та пов'язуються між
собою.

---

## Backend

- [Liquibase і Hibernate validate](content/Liquibase-і-Hibernate-validate.md): ownership схеми бази даних і forward-only міграції.
- [Контракт помилок валідації](content/Контракт-помилок-валідації.md): стабільні коди помилок і field-level violations.
- [OpenAPI, DTO та межі REST](content/OpenAPI-DTO-та-межі-REST.md): публічний API-контракт і відокремлення persistence-моделі.

## Security

- [Паролі: validation та Argon2id](content/Паролі-validation-та-Argon2id.md): різниця між password policy і безпечним зберіганням пароля.
- [JWT-аутентифікація і перевірка доступу](content/JWT-аутентифікація-і-перевірка-доступу.md): повний шлях від Bearer token у Bruno до `JwtDecoder`, `SecurityContext` і protected endpoint.
- [OpenAPI bearerAuth і SecurityRequirement](content/OpenAPI-bearerAuth-та-SecurityRequirement.md): як Swagger описує JWT-автентифікацію і чому це не замінює Spring Security.
- [@PreAuthorize на service-інтерфейсі](content/PreAuthorize-на-service-інтерфейсі.md): чому authorization для use case має бути на service boundary, а не лише в controller.
- [Refresh token rotation і reuse detection](content/Refresh-token-rotation-і-reuse-detection.md): як оновлюються refresh tokens і чому reuse завершує всю token family.
- [Logout-флоу](content/Logout-флоу.md): як завершується поточна refresh-сесія і чому access JWT ще живе до `exp`.
- [CSRF та CORS](content/CSRF-та-CORS.md): чим відрізняються browser cross-origin правила і захист cookie-based запитів.
- [OAuth2/OIDC простими словами](content/OAuth2-OIDC-простими-словами.md): як зовнішній login підтверджує identity і як VaultNote після цього видаватиме власні JWT.
- [OAuth identity та account linking](content/OAuth-identity-та-account-linking.md): чому email не є достатньою підставою для автоматичного об'єднання акаунтів.
- [OAuth-провайдери: Google, Apple, Facebook і GitHub](content/OAuth-провайдери-Google-Apple-Facebook.md): вимоги провайдерів, локальне тестування і рішення почати з Google.

## Email

- [Mail та верифікація email](content/Mail.md): межа Mail, Mailpit, SMTP і рішення поки не додавати outbox.
- [Email verification tokens](content/Email-verification-tokens.md): окрема
  модель одноразових hashed токенів для підтвердження адреси.

## Testing

- [Основні поняття тестування](content/Основні-поняття-тестування.md): рівні тестів, Angular `TestBed`, `ComponentFixture`, assertions, mocks і межа між frontend та backend-валідацією.
- [Інтеграційні тести Spring Boot](content/Інтеграційні-тести-Spring-Boot.md): RestAssured, Testcontainers, DBRider і перевірка persistence.

## Frontend

- [Структура Angular workspace](content/Структура-Angular-workspace.md): короткий огляд і навігація по frontend workspace.
- [Конфігурація та tooling Angular workspace](content/Angular-workspace-конфігурація-та-tooling.md): конфігураційні файли, npm, TypeScript, IDE і статичні ресурси.
- [Gradle-інтеграція Angular frontend](content/Angular-frontend-Gradle-інтеграція.md): запуск frontend-команд і спільний quality gate.
- [Bootstrap, routing та application shell](content/Angular-bootstrap-routing-та-application-shell.md): запуск Angular, providers, маршрути і root shell.
- [Providers, browser routing та root shell](content/Angular-application-providers-routing-root-shell.md): пояснення базових Angular application-концепцій і їхнього lifecycle.
- [Auth feature Angular frontend](content/Angular-auth-feature.md): поточна структура login feature і наступні кроки.

## Workflows

- [Gradle-модулі та пакетні межі](content/Gradle-модулі-та-пакетні-межі.md): коли достатньо одного проєкту, а коли потрібне розділення на модулі.
