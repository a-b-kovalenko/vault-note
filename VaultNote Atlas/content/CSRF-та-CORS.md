[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

CORS визначає, чи може frontend з одного origin читати відповіді API з іншого
origin. CSRF захищає state-changing запити, які браузер може виконати з
автоматично доданими cookie.

У VaultNote це різні рівні захисту:

- **CORS** дозволяє `http://localhost:4200` працювати з API на
  `http://localhost:8080`.
- **CSRF** захищає cookie-based login, refresh і logout від запитів із чужого
  сайту.

## Де це використовується у VaultNote

- **Frontend origin** — `http://localhost:4200`.
- **Backend origin** — `http://localhost:8080`.
- **Cookie credential** — `vaultnote_refresh_token`.
- **Cookie-based endpoints** — `AuthController#login`, `#refresh` і `#logout`.
- **CSRF endpoint** — `CsrfController#csrf` на `GET /csrf`.
- **CSRF cookie/header** — `XSRF-TOKEN` і `X-XSRF-TOKEN`.
- **Конфігурація CORS** — `CorsConfiguration` читає allowlist із
  `VAULTNOTE_CORS_ALLOWED_ORIGINS`.
- **Реальна перевірка** — `CsrfCorsIntegrationTest`.

## Що таке origin

Origin складається зі схеми, host і port:

```text
http://localhost:4200
│      │         │
scheme host      port
```

`http://localhost:4200` і `http://localhost:8080` мають однаковий host, але
різні порти, тому для браузера це різні origins.

## Що робить CORS

CORS — Cross-Origin Resource Sharing. Він дозволяє backend-у повідомити
браузер, які origins можуть виконувати cross-origin запити й читати відповіді.

Для VaultNote backend має дозволити конкретний frontend origin:

```text
http://localhost:4200
```

і credentials, бо refresh token зберігається в cookie:

```http
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Credentials: true
```

Якщо запит є складним, браузер спочатку виконує preflight `OPTIONS`, щоб
перевірити дозволені methods і headers.

Налаштування на кшталт `Access-Control-Allow-Origin: *` не можна поєднувати з
`Allow-Credentials: true`.

CORS відповідає на питання:

> Чи може цей frontend виконати cross-origin запит і прочитати відповідь?

Bruno, curl і Postman не обмежені browser same-origin policy, тому успішна
перевірка через Bruno ще не означає, що Angular-запит пройде без CORS.

## Що таке CSRF

CSRF — Cross-Site Request Forgery. Атака виникає, коли чужий сайт змушує
браузер користувача виконати запит до VaultNote.

Браузер може автоматично додати cookie до такого запиту:

```http
POST /api/v1/auth/logout
Cookie: vaultnote_refresh_token=...
```

Шкідливий сайт не обов'язково може прочитати відповідь, але небезпечна дія вже
може відбутися.

`HttpOnly` не захищає від CSRF. Цей прапорець лише забороняє JavaScript
прочитати cookie.

## Чому JWT менш вразливий до CSRF

Access JWT зберігається в пам'яті Angular і додається до запиту вручну:

```http
Authorization: Bearer <access-token>
```

Чужий сайт не може автоматично прочитати JWT із пам'яті іншого origin і додати
його в `Authorization` header.

Refresh token має іншу властивість: браузер додає його до відповідного запиту
автоматично. Тому cookie-backed endpoints потребують окремого CSRF-захисту.

## Поточний CSRF flow у VaultNote

Поточний flow для Angular виглядає так:

1. Angular отримує CSRF token через endpoint `/csrf`.
2. Backend записує raw token у cookie `XSRF-TOKEN` і повертає його у JSON.
3. Angular читає cookie та додає token до mutation-запитів у header
   `X-XSRF-TOKEN`.
4. `CsrfFilter` порівнює значення cookie/header із token repository.
5. Запит без коректного CSRF token відхиляється зі статусом `403`.

Spring Security захищає state-changing запити за замовчуванням. Зараз
`login`, `refresh` і `logout` вимагають CSRF token. Реєстрація та підтвердження
email явно виключені, бо вони є публічними flow до появи browser session.

Таким чином, одного автоматично доданого refresh cookie недостатньо для
успішного refresh або logout.

## CORS і CSRF — не одне й те саме

```text
CORS → чи може frontend прочитати cross-origin відповідь?
CSRF → чи справді state-changing запит ініційований нашим frontend-ом?
JWT/cookie → яким credential автентифікується користувач?
```

CORS не замінює CSRF. Навіть якщо браузер не дозволить чужому JavaScript
прочитати відповідь, це не гарантує, що state-changing запит не буде
відправлений із cookie.

## Як це реалізовано в коді

- `CsrfConfiguration` створює `CookieCsrfTokenRepository` з `HttpOnly=false`.
- `CsrfTokenRequestAttributeHandler` налаштований на пряме порівняння raw
  cookie/header значень.
- `SecurityConfig` вмикає CSRF і CORS у filter chain.
- `CorsProperties` задає allowlist через `app.cors.allowed-origins`.
- `CsrfCorsIntegrationTest` перевіряє видачу token-а, відсутній token, CORS
  preflight і невідомий origin.

Для Bruno або curl потрібно спочатку виконати `GET /csrf`, зберегти cookie
`XSRF-TOKEN` і вручну передати її значення в `X-XSRF-TOKEN`. Браузерний Angular
client зможе автоматизувати цей крок через стандартний XSRF-механізм.

## Пов'язані нотатки

- [Logout-флоу](Logout-флоу.md)
- [Refresh token rotation і reuse detection](Refresh-token-rotation-і-reuse-detection.md)
- [JWT-аутентифікація і перевірка доступу](JWT-аутентифікація-і-перевірка-доступу.md)
