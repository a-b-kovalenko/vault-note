[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

`src/app/auth/` — перша feature-папка Angular frontend для authentication flow.
Зараз вона містить контракт даних, API service і login page з reactive form,
frontend-валідацією та підключеним submit до backend; access-token state,
CSRF flow і повна обробка помилок будуть додані наступними кроками.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

## Поточні файли

### `auth.models.ts`

Frontend-моделі login request/response та raw API response. Вони описують
контракт даних між Angular і backend, але не є JPA entities.

Найближча аналогія — backend DTO або Java `record`: модель переносить дані між
межами, але не відповідає за persistence чи business workflow.

### `auth-api.service.ts`

HTTP client для `POST /api/v1/auth/login`. Service також мапить backend
`snake_case` у frontend `camelCase` і використовує `withCredentials`, щоб
браузер міг прийняти refresh-token cookie.

Його можна порівняти з невеликим gateway над `RestClient` або `WebClient`: він
відповідає за HTTP-виклик і мапінг відповіді, але не повинен перетворюватися на
backend-style сервіс із бізнес-workflow.

### `login-page.ts`

Клас і presentation logic login page, яка завантажується маршрутом `/login`.

Компонент використовує окремий `templateUrl` і `styleUrl`, тому TypeScript-файл
не містить великого inline HTML-шаблону.

Вона містить:

- reactive form з `email` і `password`;
- frontend-валідацію, синхронізовану з базовими обмеженнями `LoginRequest`;
- submit через `AuthApiService.login()`;
- loading-стан кнопки під час HTTP-запиту.

### `login-page.html`

Зовнішній Angular template login page. Він містить form controls, validation
messages, submit button, OAuth placeholders і доступні атрибути на кшталт
`aria-invalid` та `aria-describedby`.

Це UI view із presentation logic, а не аналог `@RestController`: компонент
працює у браузері й відображає стан форми.

## Наступний крок

У login feature потрібно додати:

- обробку backend validation errors, `401` та інших невдалих login responses;
- збереження access token лише в пам'яті Angular application;
- підготовку до refresh через `HttpOnly` cookie та CSRF flow.

Пізніше тут або у спільному auth infrastructure з'являться token state,
HTTP interceptors, route guards і logout behavior.
