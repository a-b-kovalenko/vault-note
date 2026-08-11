[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Logout у VaultNote завершує поточну refresh-сесію: сервер відкликає активний
refresh token і очищує його `HttpOnly` cookie. Уже виданий access JWT не
відкликається миттєво, тому він залишається чинним до завершення `exp`.

## Де це використовується у VaultNote

Logout flow проходить через такі частини застосунку:

- **HTTP endpoint** — `AuthController#logout`.
- **Керування refresh-сесією** — `RefreshTokenService#logout` і
  `RefreshTokenServiceImpl`.
- **Пошук token-а** — `RefreshTokenJpaRepository.findByTokenHash(...)`.
- **Відкликання token-а** — `RefreshTokenJpaRepository.revokeActiveById(...)`.
- **Очищення cookie** — `RefreshTokenCookieFactory#clear`.
- **HTTP-доступ** — `SecurityConfig` дозволяє `POST /api/v1/auth/logout` без Bearer access token.
- **Перевірка flow** — `RefreshTokenIntegrationTest` і Bruno request `Logout`.

## Який запит виконує клієнт

```http
POST /api/v1/auth/logout
Cookie: vaultnote_refresh_token=<raw-refresh-token>
```

Body не потрібен. Bearer access token також не потрібен: refresh token у cookie
є credential для пошуку поточної сесії.

## Що відбувається на сервері

1. `AuthController` читає refresh token із cookie, якщо cookie присутня.
2. `RefreshTokenServiceImpl` хешує raw token.
3. `RefreshTokenJpaRepository` знаходить запис за `token_hash`.
4. Якщо token активний, сервер заповнює його `revoked_at`.
5. `RefreshTokenCookieFactory` створює cookie з порожнім значенням і
   `Max-Age=0`.
6. API повертає `204 No Content`.

Після цього браузер видаляє cookie, а цей refresh token більше не може бути
використаний для отримання нового access token.

## Чому access JWT ще деякий час працює

Access JWT уже виданий і перевіряється без звернення до бази. У ньому є `exp`,
але немає механізму миттєвого відкликання. Тому після logout можливий такий
короткий перехідний стан:

```text
refresh session: revoked
access JWT:      still valid until exp
```

Це свідомий компроміс stateless JWT-підходу. Миттєве відкликання access token-ів
потребувало б denylist, token version або іншого stateful механізму.

## Чому logout ідемпотентний

Logout може прийти після того, як cookie вже видалена, token протермінований
або сесію відкликано раніше. У всіх цих випадках сервер повертає той самий
`204` і повторно надсилає команду очищення cookie.

Це корисно для клієнта: йому не потрібно окремо розбирати, чи була сесія ще
активною. Також endpoint не розкриває, чи існував переданий token.

Logout відкликає лише token поточної сесії. Інші login-сесії користувача з
іншими `token_family_id` залишаються активними.

## Що перевірити локально

1. Виконати `Login` у Bruno й отримати refresh cookie.
2. Виконати `Logout`.
3. Перевірити `204 No Content` і `Set-Cookie` з `Max-Age=0`.
4. Перевірити в `refresh_tokens`, що поточний token має `revoked_at`.
5. Повторити `Refresh Token` зі старим raw token і отримати
   `REFRESH_TOKEN_AUTHENTICATION_FAILED`.

Повторний `Logout` без cookie також має повернути `204`.

## CSRF для browser flow

Cookie-based logout тепер захищений CSRF. Browser client спочатку отримує token
через `GET /csrf`, читає cookie `XSRF-TOKEN` і передає її значення в header
`X-XSRF-TOKEN` разом із refresh cookie. Без коректного header сервер відхиляє
запит зі статусом `403`.

## Пов'язані нотатки

- [CSRF та CORS](CSRF-та-CORS.md)
- [Refresh token rotation і reuse detection](Refresh-token-rotation-і-reuse-detection.md)
- [JWT-аутентифікація і перевірка доступу](JWT-аутентифікація-і-перевірка-доступу.md)
