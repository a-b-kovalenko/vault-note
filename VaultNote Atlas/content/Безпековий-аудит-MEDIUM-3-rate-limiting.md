[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `PARTIALLY RESOLVED`  
**Рівень:** `MEDIUM`

Публічні registration і password-reset endpoint-и поки не мають server-side
rate limiting. Login уже захищений лімітами за IP та нормалізованим email, але
решта публічних auth-flow ще може запускати дорогі операції без обмеження.

## У чому проблема

CSRF і CORS не обмежують CLI, bot або прямі HTTP-запити. Клієнт може отримати
CSRF token і надсилати багато запитів до публічних endpoint-ів.

Найбільш дорогі сценарії:

- login запускає перевірку пароля через Argon2;
- registration виконує Argon2, запис у PostgreSQL і SMTP-відправлення;
- password reset шукає акаунт, створює token і може надсилати email.

## Ризик

Можливі credential stuffing, CPU/memory exhaustion, масове створення акаунтів,
SMTP abuse і denial-of-service для password recovery.

## Як це потрібно вирішити

План передбачає:

- ліміт за IP і нормалізованим email для login;
- IP та email/device quota для registration;
- IP та email limit для password reset;
- однакове застосування email-ліміту для існуючих і неіснуючих акаунтів;
- перевірку ліміту до Argon2, database writes і SMTP;
- `429 Too Many Requests` та `Retry-After`;
- generic error без account enumeration;
- жодного постійного блокування акаунта.

Для одного локального процесу достатньо bounded in-memory storage з expiry. Для
кількох backend-інстансів потрібне shared atomic storage, наприклад Redis, або
rate limiting на edge/WAF.

## Що вже реалізовано для login

- `LoginService` перевіряє ліміти до пошуку користувача та Argon2.
- Ліміти застосовуються одночасно за IP і нормалізованим email.
- Для локального запуску використовується bounded in-memory store з expiry,
  cleanup і максимальним числом записів.
- Перевищення повертає `429 Too Many Requests`, generic
  `RATE_LIMIT_EXCEEDED` і `Retry-After`.
- Unit-тести перевіряють atomic behavior, expiry, нормалізацію та відсутність
  виклику login-процесу після відмови; integration test перевіряє HTTP endpoint.

## Що ще потрібно перевірити

Потрібні unit-тести з controllable clock, integration tests для трьох endpoint-ів
і перевірка, що після `429` не виконуються Argon2, repository та mail sender.
Також потрібно додати метрики або audit events, не записуючи паролі, raw tokens
і повні email-адреси в logs.

## Поточний висновок

Login-частину finding закрито для одного локального процесу. Потрібно додати
обмеження для registration і password reset, а для кількох backend-інстансів
вибрати shared atomic storage або edge/WAF. До цього backend не слід виставляти
в недовірену публічну мережу.
