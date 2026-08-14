[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `PARTIALLY RESOLVED`  
**Рівень:** `MEDIUM`

Login, registration і password-reset request тепер мають server-side rate
limiting за IP та нормалізованим email. Поточне сховище — bounded in-memory
store для одного JVM-процесу; shared storage для кількох backend-інстансів ще
не підключено.

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
кількох backend-інстансів потрібне shared atomic storage. Для VaultNote
цільовим storage обрано PostgreSQL, а edge/WAF потрібен для coarse IP flood
protection.

## Що вже реалізовано

- `LoginService` перевіряє ліміти до пошуку користувача та Argon2.
- `RegistrationService` перевіряє ліміти до Argon2, запису користувача та
  verification email.
- `PasswordResetService` перевіряє ліміти до пошуку користувача, створення
  reset token і відправлення email. Email-лічильник збільшується навіть для
  невідомого користувача.
- Усі ліміти застосовуються одночасно за IP і нормалізованим email.
- `RateLimitScope` обʼєднує login, registration і password reset в один
  типобезпечний `RateLimitService.check(...)` без raw scope strings у callers.
- Для локального запуску використовується bounded in-memory store з expiry,
  cleanup і максимальним числом записів.
- Перевищення повертає `429 Too Many Requests`, generic
  `RATE_LIMIT_EXCEEDED` і `Retry-After`.
- Unit- та integration-тести перевіряють пороги, expiry, нормалізацію,
  generic response і відсутність downstream side effects після відмови.

## Що ще потрібно зробити

- Перевести counters із in-memory у shared atomic storage після розбору
  нестабільної PostgreSQL-гілки.
- Додати failure policy для недоступного shared storage і вирішити, чи
  дозволяти запити при помилці ліміту.
- Додати edge/WAF для coarse IP flood protection у публічному deployment.
- Додати метрики або audit events, не записуючи паролі, raw tokens і повні
  email-адреси в logs.

## Поточний висновок

Login, registration і password-reset request захищені для одного локального
процесу. In-memory counters скидаються після restart і не синхронізуються між
інстансами, тому до підключення shared storage та edge/WAF backend не слід
виставляти в недовірену публічну мережу.

## Історія гілки `feat/postgres-rate-limit-storage`

Ця гілка була створена, щоб реалізувати наступний етап `MEDIUM-3` — спільне
PostgreSQL-сховище для rate limiting login і registration. Гілка залишилася
окремою і не була злита в `main`.

### Що сталося

Локально targeted integration tests і повний `./gradlew check --no-daemon`
проходили. У CI rate-limit integration tests продовжували падати
непослідовно: у різних запусках не спрацьовувала очікувана відмова після
досягнення ліміту для login, registration або прямого PostgreSQL store.

Точну причину не встановлено. Найбільш підозрілими залишилися активація
конфігурації rate limiting, межі транзакцій і test/transaction isolation у CI.

### Зміни гілки

- `99029ef` — counters перенесено з in-memory підходу в PostgreSQL; додано
  Liquibase-схему, atomic store, limits для login і registration та тести.
- `e8dcb8b` — у rate-limit integration tests явно увімкнено
  `app.security.rate-limit.enabled=true`, щоб тестовий profile не вимикав
  перевірку випадково.
- `832dd29` — production `PostgresRateLimitStore` переведено на окремий
  `JdbcTransactionManager` з `REQUIRES_NEW`, щоб commit counter-а був фізично
  відокремлений від JPA-транзакції login або registration.

Після обох спроб локальні перевірки проходили, але стабільного CI-результату
отримати не вдалося. Тому цю гілку призупинено, а `MEDIUM-3` не можна вважати
повністю закритим. Наступне розслідування потрібно почати з актуальних
CI-логів для `832dd29`, відтворення конкретного сценарію в PostgreSQL
integration test і перевірки меж транзакцій та ізоляції тестів.

## Історія гілки `feat/password-reset-rate-limiting`

Ця гілка додала password-reset rate limiting поверх поточного in-memory
storage, не змінюючи тимчасову модель локального запуску.

### Що було зроблено

- Додано окремі configurable IP/email limits для password reset із типовими
  значеннями 20 запитів з IP та 3 запити для email за одну годину.
- Перевірку підключено в `PasswordResetService`, до пошуку користувача,
  створення token і відправлення email.
- Невідомі email споживають email-ліміт, але отримують ту саму generic відповідь,
  що й відомі email до моменту перевищення ліміту.
- Додано `429`, `Retry-After`, unit-тести та PostgreSQL integration test для
  endpoint-а з in-memory rate-limit store.
- Замість трьох майже однакових методів використано один
  `RateLimitService.check(...)` з enum `RateLimitScope`.

Повний `./gradlew check --no-daemon` проходить. Ця гілка не вирішує проблему
shared storage для multi-instance deployment; вона закриває password-reset
частину поведінки для поточного локального in-memory режиму.
