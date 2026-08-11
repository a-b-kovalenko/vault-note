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

## Email

- [Mail та верифікація email](content/Mail.md): межа Mail, Mailpit, SMTP і рішення поки не додавати outbox.
- [Email verification tokens](content/Email-verification-tokens.md): окрема
  модель одноразових hashed токенів для підтвердження адреси.

## Testing

- [Інтеграційні тести Spring Boot](content/Інтеграційні-тести-Spring-Boot.md): RestAssured, Testcontainers, DBRider і перевірка persistence.

## Workflows

- [Gradle-модулі та пакетні межі](content/Gradle-модулі-та-пакетні-межі.md): коли достатньо одного проєкту, а коли потрібне розділення на модулі.
