[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Refresh token — це довгоживучий credential, за допомогою якого клієнт отримує новий короткоживучий access token. Під час кожного refresh старий refresh token відкликається, а замість нього створюється новий у тій самій `token family`.

Якщо вже відкликаний токен використати повторно, це схоже на викрадення токена. Тоді застосунок відкликає всі ще активні токени цієї family і завершує сесію.

## Де це використовується у VaultNote

У нашому застосунку ця концепція реалізується в кількох шарах:

- **Зберігання refresh token** — `004-create-refresh-tokens-table.xml` створює таблицю `refresh_tokens`.
- **Token family** — колонка `token_family_id` є частиною migration і `RefreshTokenEntity`.
- **Hash замість raw token** — колонка `token_hash` має довжину 64 символи та унікальне обмеження.
- **Стан відкликання** — `revoked_at` показує, чи був токен відкликаний.
- **JPA-модель** — `RefreshTokenEntity`.
- **Пошук із захистом від race condition** — `RefreshTokenJpaRepository.findByTokenHash(...)` використовує `PESSIMISTIC_WRITE`.
- **Відкликання всієї family** — `RefreshTokenJpaRepository.revokeActiveByTokenFamilyId(...)` оновлює всі активні токени family одним запитом.
- **Refresh endpoint** — `AuthController#refresh` приймає raw token із cookie та повертає нову пару токенів.
- **Ротація** — `RefreshTokenServiceImpl` відкликає використаний токен і створює наступний у тій самій family.
- **Виявлення reuse** — повторне використання відкликаного токена відкликає всі активні токени його family.
- **Інтеграційна перевірка** — `RefreshTokenIntegrationTest` перевіряє HTTP-flow і стан PostgreSQL після ротації.

Refresh flow реалізований поверх цієї persistence-моделі. Endpoint дозволений без Bearer access token, бо автентифікація відбувається за refresh token у `HttpOnly` cookie.

## Навіщо потрібна rotation

Якщо refresh token живе сім днів і ніколи не змінюється, його викрадення дає атакеру можливість оновлювати access token протягом усього цього терміну.

Rotation зменшує це вікно:

1. клієнт надсилає поточний refresh token;
2. сервер перевіряє його та відкликає;
3. сервер створює новий refresh token;
4. новий токен передається клієнту через `HttpOnly` cookie.

Тому кожен refresh token можна використати лише один раз.

## Що таке token family

`token_family_id` — це ідентифікатор ланцюжка токенів, які належать одній сесії. Він не є самим токеном і не передається клієнту як credential.

Наприклад, після login створюється family `F1`:

```text
F1: T1 → T2 → T3 → T4
```

Усі ці токени мають однаковий `token_family_id`, але різні хеші. Кожен новий токен замінює попередній.

Окремий login створює іншу family. Тому відкликання однієї сесії не повинно автоматично завершувати всі сесії користувача на інших пристроях.

## Нормальний refresh

Початковий стан:

```text
T1: active
T2: не існує
```

Клієнт надсилає `T1`. Сервер:

1. хешує raw token із cookie;
2. знаходить запис за `token_hash`;
3. бере pessimistic lock на цей запис;
4. перевіряє, що токен не відкликаний і не прострочений;
5. встановлює для `T1` значення `revoked_at`;
6. створює `T2` з тією самою `token_family_id`;
7. повертає новий access token і замінює cookie на `T2`.

Після цього стан такий:

```text
T1: revoked
T2: active
```

Pessimistic lock потрібен, щоб два паралельні запити не змогли одночасно
прийняти один і той самий `T1` як активний.

## Refresh endpoint

Клієнт викликає `POST /api/v1/auth/refresh`. Raw refresh token передається
автоматично в cookie `vaultnote_refresh_token`; access token у заголовку
`Authorization` для цього endpoint-а не потрібен.

Успішна відповідь містить новий access token у JSON і замінює refresh cookie
наступним raw token. Запит є `permitAll` у `SecurityConfig`, але це не робить
його публічним у практичному сенсі: без валідного cookie сервіс повертає
`401`.

Перевірки й зміни виконуються в одній транзакції:

1. raw token хешується;
2. запис шукається під pessimistic lock;
3. перевіряються `revoked_at` і `expires_at`;
4. поточний token відкликається атомарним update;
5. створюється наступний token у тій самій family;
6. генерується новий access token і встановлюється cookie.

Якщо token невідомий, прострочений або вже відкликаний, API повертає
`REFRESH_TOKEN_AUTHENTICATION_FAILED`. Для вже відкликаного token-а додатково
відкликаються всі активні token-и його family.

## Що відбувається при reuse

Уявімо, що атакер скопіював `T1`, а справжній клієнт уже виконав refresh:

```text
T1: revoked
T2: active
```

Атакер повторно надсилає `T1`. Сервер знаходить його, але бачить, що
`revoked_at` уже заповнений. Це сигнал reuse — повторного використання
одноразового токена.

У відповідь сервер викликає логіку на кшталт:

```sql
UPDATE vaultnote.refresh_tokens
SET revoked_at = CURRENT_TIMESTAMP
WHERE token_family_id = :familyId
  AND revoked_at IS NULL;
```

Таким чином відкликається і `T2`, а вся сесія `F1` стає недійсною. Атакер не
може продовжити сесію, а справжній клієнт має пройти login повторно.

Метод `revokeActiveByTokenFamilyId` потрібен саме для цього сценарію. Він не
викликається під час кожного звичайного refresh — тоді відкликається лише
поточний токен.

## Чому зберігається hash

Raw refresh token живе тільки в `HttpOnly` cookie клієнта. У базі зберігається
його SHA-256 hash:

```text
cookie:     raw-refresh-token
database:   SHA-256(raw-refresh-token)
```

Якщо база даних буде скомпрометована, значення з таблиці не можна одразу
використати як готовий refresh token.

## Основні стани токена

- `active` — токен можна використати, якщо він не прострочений.
- `revoked` — токен уже замінено або відкликано через reuse.
- `expired` — `expires_at` минув, навіть якщо `revoked_at` порожній.
- `reuse detected` — надійшла спроба використати токен, який уже відкликаний.

У таблиці немає текстового статусу. Стан визначається комбінацією
`expires_at` і `revoked_at`.

## Важлива транзакційність

Перевірка токена, його відкликання та створення наступного токена мають виконуватися в одній транзакції. Пошук із `PESSIMISTIC_WRITE` lock і bulk-метод для відкликання family також мають викликатися з транзакційного сервісу.
