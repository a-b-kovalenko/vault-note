[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

`src/app/auth/` — перша feature-папка Angular frontend для authentication flow.
Вона містить OpenAPI-моделі, API service, login page з reactive form,
in-memory access-token state, CSRF flow, bearer interceptor і автоматичний
single-flight refresh через `HttpOnly` cookie.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

## Поточні файли

### `auth.models.ts`

Frontend-моделі login request/response та raw API response, згенеровані з
OpenAPI-контракту backend. Вони описують контракт даних між Angular і backend,
але не є JPA entities.

Найближча аналогія — backend DTO або Java `record`: модель переносить дані між
межами, але не відповідає за persistence чи business workflow.

### `auth-api.service.ts`

HTTP gateway для `POST /api/v1/auth/login` і `POST /api/v1/auth/refresh`.
Перед state-changing запитом service гарантує наявність CSRF cookie, мапить
backend `snake_case` у frontend `camelCase` і використовує `withCredentials`,
щоб браузер міг прийняти та надсилати refresh-token cookie.

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

### `auth-state.service.ts`

`AuthStateService` — application-wide in-memory state для поточної Angular
сесії. Він зберігає access token, `tokenType` і `expiresIn` лише в signal-пам'яті
та надає `setSession`, `clearSession`, `accessToken` і `isAuthenticated`.

Access token навмисно не записується в `localStorage`, `sessionStorage`, URL або
інше persistent browser storage. Після повного перезавантаження сторінки цей
state порожній, а відновлення виконується через refresh cookie.

## Що означає CSRF bootstrap та refresh/interceptor

Цей крок з'єднує login API, browser cookies і access-token state в один
безпечний flow. Тут є два різні механізми:

- CSRF захищає state-changing запити, які використовують cookies;
- bearer interceptor додає access JWT до захищених запитів і запускає refresh
  після `401`.

### CSRF bootstrap

`CsrfService` перед login або refresh викликає `GET /csrf` з
`withCredentials: true`. Backend відповідає CSRF token і встановлює readable
cookie `XSRF-TOKEN`.

Refresh cookie має іншу властивість: він називається
`vaultnote_refresh_token`, є `HttpOnly` і недоступний JavaScript. Це правильно:
frontend не повинен читати refresh token, але браузер може автоматично надсилати
його backend за умови `withCredentials`.

`csrf.interceptor.ts` читає `XSRF-TOKEN` cookie та для `POST`, `PUT`, `PATCH` і
`DELETE` додає header `X-XSRF-TOKEN`. Саме пару cookie + header перевіряє
Spring Security.

### Bearer interceptor

`auth.interceptor.ts` додає до backend-запитів:

- `Authorization: Bearer <access-token>` із `AuthStateService`;
- `withCredentials: true`, щоб працював refresh cookie.

Public auth endpoints (`login`, `refresh`, `logout`, registration і CSRF
bootstrap) не отримують зайвий bearer token і не запускають refresh loop.

### Refresh після `401`

Якщо захищений запит повернув `401`, interceptor передає refresh у
`AuthRefreshService`:

1. service викликає `POST /api/v1/auth/refresh`;
2. backend перевіряє `HttpOnly` refresh cookie, відкликає старий token і видає
   новий access token та нову cookie;
3. нова access-сесія записується в `AuthStateService`;
4. початковий HTTP-запит повторюється з новим bearer token.

`AuthRefreshService` використовує single-flight поведінку. Якщо кілька запитів
одночасно отримали `401`, вони очікують один спільний refresh, а не створюють
кілька refresh-запитів і не провокують зайві rotation операції.

Якщо refresh завершився помилкою, access state очищується. Frontend більше не
вважає користувача authenticated і може передати керування login flow.

### Послідовність login flow

1. `LoginPage` передає credentials в `AuthApiService`.
2. `CsrfService` отримує CSRF cookie, якщо вона ще не підготовлена.
3. CSRF interceptor додає `X-XSRF-TOKEN` до login-запиту.
4. Backend повертає access JWT і встановлює `HttpOnly` refresh cookie.
5. `LoginPage` передає response в `AuthStateService`.
6. Наступні protected requests отримують bearer header автоматично.
7. Після завершення TTL interceptor оновлює access token через refresh і
   повторює початковий запит.

## Що залишається

У login feature потрібно додати:

- route guards для authenticated і administrator routes;
- logout flow у frontend та очищення auth state;
- єдине відображення стандартних `ProblemDetail` помилок;
- protected Notes, profile та administrator screens.

Browser e2e-тести навмисно залишаються поза цією фазою.
