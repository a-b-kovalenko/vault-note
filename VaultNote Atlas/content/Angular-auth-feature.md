[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

`src/app/auth/` — перша feature-папка Angular frontend для authentication flow.
Зараз вона містить контракт даних, API service і тимчасову login page; reactive
form та повний auth state будуть додані наступним кроком.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

## Поточні файли

### `auth.models.ts`

Frontend-моделі login request/response та raw API response. Вони описують
контракт даних між Angular і backend, але не є JPA entities.

Найближча аналогія — backend DTO або Java `record`: модель переносить дані між
межами, але не відповідає за persistence чи business workflow.

### `auth-api.service.ts`

HTTP client для `POST /api/v1/auth/login`. Service також мапить backend
`snake_case` у frontend `camelCase`.

Його можна порівняти з невеликим gateway над `RestClient` або `WebClient`: він
відповідає за HTTP-виклик і мапінг відповіді, але не повинен перетворюватися на
backend-style сервіс із бізнес-workflow.

### `login-page.ts`

Тимчасовий routed placeholder, який завантажується маршрутом `/login`.

Це UI view із presentation logic, а не аналог `@RestController`: компонент
працює у браузері й відображає стан форми.

## Наступний крок

У login feature потрібно додати:

- reactive form для email і password;
- виклик `auth-api.service.ts` після submit;
- обробку loading та validation errors;
- збереження access token лише в пам'яті Angular application;
- підготовку до refresh через `HttpOnly` cookie та CSRF flow.

Пізніше тут або у спільному auth infrastructure з'являться token state,
HTTP interceptors, route guards і logout behavior.
