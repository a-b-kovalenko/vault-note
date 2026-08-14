[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `NEEDS VERIFICATION`  
**Тип:** deployment hardening

У конфігурації є development-friendly default-и: refresh cookie може мати
`Secure=false`, а SMTP STARTTLS може бути вимкнений. Це не підтверджена
вразливість production, бо production deployment ще не описаний, але реальні
runtime settings потрібно перевірити перед зовнішнім запуском.

## Що саме потрібно перевірити

Потрібно знати фактичну схему розгортання:

- чи весь трафік іде через HTTPS;
- де завершується TLS — у reverse proxy чи в самому backend;
- чи передаються правильні forwarded headers;
- чи в production встановлено `Secure=true` для refresh cookie;
- чи SMTP використовує TLS/STARTTLS і перевірку сертифіката;
- чи issuer JWT, CORS origins та інші environment variables відповідають
  реальному домену.

## Ризик

Якщо залишити development settings у production:

- cookie може передаватися через незашифрований HTTP;
- SMTP credentials або email traffic можуть бути незахищеними;
- невірні proxy settings можуть зламати security decisions;
- CORS або JWT issuer можуть дозволити небажані origins чи токени.

## Поточний стан

Документація позначає ці default-и як development-only, але цього недостатньо
для production гарантії. Реального deployment target у репозиторії немає,
тому проблему ще не можна остаточно закрити.

## Як закрити

1. Описати фактичний production deployment і reverse-proxy configuration.
2. Зробити fail-fast validation для небезпечних production settings.
3. Перевірити HTTPS, `Secure` cookie, SMTP TLS, issuer і CORS allowlist у
   production-like integration або smoke tests.
4. Зберігати секрети та environment values у secret manager/deployment
   configuration, а не в репозиторії.
