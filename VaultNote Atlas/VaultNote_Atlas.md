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
- [Refresh token rotation і reuse detection](content/Refresh-token-rotation-і-reuse-detection.md): як оновлюються refresh tokens і чому reuse завершує всю token family.

## Email

- [Mail та верифікація email](content/Mail.md): межа Mail, Mailpit, SMTP і рішення поки не додавати outbox.
- [Email verification tokens](content/Email-verification-tokens.md): окрема
  модель одноразових hashed токенів для підтвердження адреси.

## Testing

- [Інтеграційні тести Spring Boot](content/Інтеграційні-тести-Spring-Boot.md): RestAssured, Testcontainers, DBRider і перевірка persistence.

## Workflows

- [Gradle-модулі та пакетні межі](content/Gradle-модулі-та-пакетні-межі.md): коли достатньо одного проєкту, а коли потрібне розділення на модулі.
