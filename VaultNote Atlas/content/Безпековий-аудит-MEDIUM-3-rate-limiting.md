[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `PARTIALLY RESOLVED`  
**Рівень:** `MEDIUM`

Login і registration тепер мають IP- та normalized-email-aware rate limiting.
Лічильники зберігаються тільки в PostgreSQL і працюють однаково під час
локального запуску з IDE та в deployment. Password reset ще потрібно захистити
окремим лімітом.

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

## Загальний принцип рішення

Rate limiting не блокує акаунт назавжди. Він обмежує частоту запитів за двома
незалежними ключами:

- IP-адреса клієнта;
- нормалізований email — обрізані пробіли та lower case.

Перевірка відбувається до Argon2, пошуку користувача, database writes і SMTP.
Після перевищення повертається `429 Too Many Requests`, `Retry-After` і
нейтральний код `RATE_LIMIT_EXCEEDED`, без пояснення, який саме ключ спрацював.

## Чому storage — тільки PostgreSQL

PostgreSQL уже є обов’язковою залежністю локального backend, тому окремий Redis
для rate limiting не потрібен. Усі backend-інстанси використовують спільну
таблицю `vaultnote.rate_limit_counters`.

Для кожного ліміту зберігаються hashed key, кількість запитів і час завершення
вікна. Ліміт перевіряється транзакційно:

- row створюється через `INSERT ... ON CONFLICT DO NOTHING`;
- рядок блокується через `SELECT ... FOR UPDATE`;
- IP та email counters обробляються в детермінованому порядку;
- якщо будь-який ліміт перевищено, вся операція відкочується;
- якщо всі правила дозволяють запит, counters оновлюються разом.

Тому кілька backend-інстансів не мають власних розрізнених лічильників.
Локальний запуск з IDE використовує ту саму PostgreSQL-таблицю, що й
deployment.

## Чому `JdbcTemplate`, а не JPA repository

Це не звичайний CRUD над domain entity. Rate-limit counter потребує
PostgreSQL-specific atomic operations, row locking і контролю порядку оновлення.

Звичайна схема через JPA repository:

```text
find counter → перевірити limit → змінити entity → save
```

не є безпечною під паралельними запитами. Два потоки можуть прочитати однакове
значення. Щоб зробити JPA-варіант коректним, усе одно знадобилися б entity,
pessimistic locking, native `ON CONFLICT` queries і обробка race під час
першої вставки.

`JdbcTemplate` тут використано свідомо:

- SQL і `FOR UPDATE` видно безпосередньо;
- немає JPA first-level cache, dirty checking або зайвої domain entity;
- PostgreSQL-specific поведінка залишається в одному infrastructure adapter;
- `RateLimitStore` зберігає application contract, тому service не залежить від
  деталей JDBC.

Це не означає, що repository підхід неможливий. Він просто додав би JPA-шар,
але не прибрав би потребу в тому самому native SQL для атомарності.

## Чому використано `REQUIRES_NEW`

Rate-limit counter має зберегтися навіть тоді, коли сам login або registration
завершилися помилкою.

Наприклад:

- неправильний пароль викликає rollback login transaction;
- duplicate email викликає rollback registration transaction.

Якби counter оновлювався в цій самій транзакції, невдала спроба не рахувалася б.
Тому PostgreSQL store комітить rate-limit операцію в окремій JDBC-транзакції
через `JdbcTransactionManager` з propagation `REQUIRES_NEW`. Це відокремлює
лічильник від JPA-транзакції login або registration на рівні фізичного
PostgreSQL connection.

## Що вже реалізовано

- Login має IP та normalized-email limits до пошуку користувача й Argon2.
- Registration має IP та normalized-email limits до Argon2, PostgreSQL write і
  verification email.
- PostgreSQL counter table додана через Liquibase changeset `007`.
- Ключі зберігаються як SHA-256 hashes, а не як raw email або IP.
- Невдалі downstream-операції не відкочують counters.
- Додані unit-тести для atomic behavior, expiry, rollback і rejected decisions.
- Доданий PostgreSQL Testcontainers integration test.
- Повний `./gradlew check` проходить.

## Що ще потрібно зробити

- Додати IP та normalized-email rate limiting для
  `POST /api/v1/auth/password-reset/request`.
- Додати cleanup прострочених counter rows.
- Визначити failure policy для недоступного PostgreSQL.
- Додати edge/WAF для coarse IP flood protection у публічному deployment.
- Додати низькокардинальні metrics або audit events без паролів, raw tokens і
  повних email-адрес у logs.

До завершення цих пунктів backend не слід виставляти в недовірену публічну
мережу.
