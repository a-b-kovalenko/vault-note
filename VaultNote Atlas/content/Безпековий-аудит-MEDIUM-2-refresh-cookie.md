[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `RESOLVED`  
**Рівень:** `MEDIUM`

Назва refresh cookie вже була конфігурованою під час її створення, але `refresh`
і `logout` читали тільки hardcoded `vaultnote_refresh_token`. Через це custom
назва ламала refresh, а logout міг очистити cookie у браузері, не відкликавши
token у базі. Тепер читання і створення cookie використовують одне джерело
конфігурації.

## У чому була проблема

Назва cookie задається через:

```text
VAULTNOTE_REFRESH_TOKEN_COOKIE_NAME
```

`RefreshTokenCookieFactory` створював cookie з цією назвою. Але контролер
очікував:

```java
@CookieValue(name = "vaultnote_refresh_token")
```

Тобто запис і читання могли використовувати різні назви.

## Що відбувалося з custom назвою

Якщо назву змінити на `custom_refresh_token`:

- login встановлював `custom_refresh_token`;
- refresh не знаходив cookie і не передавав token у сервіс;
- logout отримував `null` і не відкликав token у PostgreSQL;
- браузер видаляв cookie, але викрадений raw token міг залишатися активним.

## Як проблему вирішено

- Додано `RefreshTokenCookieExtractor`.
- Extractor шукає cookie через `RefreshTokenProperties.cookieName()`.
- `AuthController` використовує extractor для refresh і logout.
- Для refresh відсутня cookie перетворюється на стандартну `401` помилку.
- Для logout відсутня cookie залишається ідемпотентним сценарієм.
- Назва cookie проходить startup validation на blank і некоректні символи.

## Як перевірено

- Unit-тест extractor перевіряє custom назву, іншу назву та відсутність cookies.
- `RefreshTokenIntegrationTest` запускається з
  `app.refresh-token.cookie-name=custom_refresh_token`.
- Перевірено login, refresh, logout, database revocation і видалення cookie.
- Старий raw token після logout не приймається ні під custom, ні під default
  назвою.

## Головний висновок

Конфігуроване значення має бути єдиним джерелом і для запису, і для читання,
і для очищення cookie. Інакше deployment override може змінити поведінку
автентифікації та logout.
