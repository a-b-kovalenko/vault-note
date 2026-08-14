[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `NEEDS VERIFICATION`  
**Тип:** deployment hardening

`compose.yaml` публікує SMTP і Mailpit UI без явного loopback bind. На спільній
мережі це може відкрити UI та email links із reset/verification листів іншим
людям. Для локальної машини це зручно, але фактичний bind і firewall потрібно
перевірити.

## У чому ризик

Mailpit містить листи, які можуть мати:

- password-reset links;
- email-verification tokens;
- адреси користувачів та інший локальний тестовий контент.

Якщо порт доступний не лише з `127.0.0.1`, інший процес або користувач у
спільній мережі може читати ці листи або використовувати посилання з них.

## Чому це ще не підтверджена вразливість

У звіті немає production deployment, host firewall або фактичної bind
конфігурації. Тому спочатку потрібно перевірити, на які адреси реально
публікуються SMTP та UI порти.

## Як закрити

- Для локальної розробки bind-ити порти на `127.0.0.1`, якщо доступ з мережі не
  потрібен.
- Якщо потрібен network access, додати firewall або authentication/access
  control.
- Не запускати Mailpit як production mail service.
- Перевірити, що reset і verification links не доступні з зовнішньої мережі.
