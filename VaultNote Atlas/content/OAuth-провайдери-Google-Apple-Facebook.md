[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Для OAuth/OIDC потрібен не окремий акаунт VaultNote, а налаштований client у
кожного зовнішнього провайдера. Для першої реалізації обираємо Google: достатньо
звичайного Google Account і Google Cloud Project, без платної developer-програми.
Apple відкладаємо через обов'язкове членство в Apple Developer Program, а
Facebook і GitHub залишаємо можливими наступними провайдерами.

## Навіщо порівнювати провайдерів

OAuth/OIDC flow у VaultNote загалом однаковий для різних провайдерів:

1. VaultNote перенаправляє користувача до провайдера.
2. Провайдер автентифікує користувача і повертає authorization code.
3. Backend обмінює code на provider tokens і перевіряє identity користувача.
4. VaultNote знаходить або створює локального User.
5. VaultNote видає власні access JWT і refresh token.

Відрізняються головним чином вимоги до реєстрації застосунку, налаштування
redirect URI, client credentials і правила доступу до production.

## Google

### Що потрібно для Google

- звичайний Google Account;
- Google Cloud Project, створений під цим акаунтом;
- налаштований OAuth consent screen;
- OAuth Client типу `Web application`;
- `client_id` і `client_secret`;
- зареєстрований redirect URI.

Для локальної розробки Google дозволяє redirect URI на `localhost`, наприклад:

```text
http://localhost:8080/login/oauth2/code/google
```

Якщо застосунок має External audience і перебуває у статусі Testing, акаунт,
яким ми тестуємо login, потрібно додати до списку test users. Це може бути той
самий особистий Google Account, яким створено проєкт.

Для нашого login достатньо мінімальних scopes:

```text
openid
email
profile
```

### Чого не потрібно

- окремого Google Developer Account;
- платної developer-підписки;
- окремого Google Workspace домену;
- зберігати пароль Google у VaultNote.

Google Account і Google Cloud Project — це не два різні акаунти. Project є
ресурсом, яким керує власник Google Account.

Для локального запуску не плануємо підключати платні Google API. OAuth client,
consent screen та базовий OIDC login — окрема частина від використання інших
Google Cloud API, для яких можуть діяти власні вимоги.

### Що знадобиться перед production

- production redirect URI на реальному HTTPS-домені;
- коректно заповнений consent screen;
- privacy policy та інші дані, якщо їх вимагатиме Google для публічного запуску;
- перевірка застосунку, якщо додамо scopes або сценарії, для яких вона потрібна.

Офіційні матеріали:

- [OAuth 2.0 для web server applications](https://developers.google.com/identity/protocols/oauth2/web-server)
- [OpenID Connect для Sign in with Google](https://developers.google.com/identity/openid-connect/openid-connect)
- [Налаштування audience і test users](https://support.google.com/cloud/answer/15549945)

## Apple

### Що потрібно для Apple

- Apple Account;
- членство в Apple Developer Program;
- App ID з увімкненим Sign in with Apple;
- Services ID для web login;
- private key і client secret;
- налаштовані домени та redirect URI.

Apple Developer Program є платним членством, зазвичай із річною оплатою. Тому
для першого OAuth-провайдера ми Apple не обираємо.

Є ще практичні особливості:

- користувач може приховати email і отримати Apple private relay address;
- email не можна вважати стабільним ідентифікатором;
- identity потрібно зберігати за парою `provider + subject`;
- web flow потребує додаткового налаштування домену.

Офіційні матеріали:

- [Apple Developer Program](https://developer.apple.com/programs/enroll/)
- [Налаштування Sign in with Apple для web](https://developer.apple.com/help/account/capabilities/configure-sign-in-with-apple-for-the-web)

## GitHub

### Важлива відмінність

GitHub OAuth App використовує OAuth 2.0, але не є повноцінним OIDC login:
GitHub не видає `id_token` у цьому flow. Після обміну authorization code backend
отримує access token і запитує identity через GitHub API.

### Що потрібно для GitHub

- GitHub Account із підтвердженим email;
- OAuth App у GitHub Developer settings;
- Client ID і Client Secret;
- Authorization callback URL;
- мінімальні scopes `read:user` і `user:email`.

`read:user` дає доступ до профілю через `GET /user`, а `user:email` потрібен,
якщо email користувача приватний і його треба отримати через `GET /user/emails`.

Для web flow GitHub рекомендує використовувати `state` і PKCE. Authorization
code одноразовий і має короткий строк дії.

Окреме платне developer-членство для GitHub OAuth App не потрібне.

### Особливості інтеграції

На відміну від Google, у GitHub flow backend не перевіряє OIDC `id_token`. Він:

1. перевіряє `state`;
2. обмінює code на GitHub access token;
3. викликає GitHub API для профілю та email;
4. бере стабільний GitHub user ID як `provider_subject`;
5. далі використовує звичайний VaultNote login flow.

Не можна використовувати email як зовнішній ідентифікатор: користувач може
змінити основний email або приховати його.

Офіційні матеріали:

- [Створення OAuth App](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app)
- [Авторизація OAuth App](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- [Scopes OAuth App](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps)
- [Профіль автентифікованого користувача](https://docs.github.com/en/rest/users/users#get-the-authenticated-user)
- [Email-адреси користувача](https://docs.github.com/en/rest/users/emails#list-email-addresses-for-the-authenticated-user)

## Facebook

### Що потрібно для Facebook

- Facebook або Meta Account із доступом до Meta for Developers;
- Meta app;
- продукт Facebook Login;
- App ID і App Secret;
- зареєстрований OAuth redirect URI;
- налаштовані дозволи, мінімально `public_profile` і, якщо потрібно,
  `email`.

Окремий Apple-подібний developer membership для Facebook Login не потрібен,
але потрібен доступ до Meta for Developers і створення застосунку. Для
production можуть знадобитися додаткові перевірки застосунку залежно від
дозволів та сценаріїв використання.

Як і для інших провайдерів, Facebook user ID треба зберігати як зовнішню
identity, а не підміняти ним локальний `userId`.

Офіційний матеріал:

- [Facebook Login for Web](https://developers.facebook.com/docs/facebook-login/web/)

## Як зберігати identity у VaultNote

Email не є достатнім ключем для зовнішньої identity. Зовнішній зв'язок має бути
побудований приблизно так:

```text
provider = google
provider_subject = 123456789
user_id = 42
```

Для Apple це особливо важливо через private relay email, але правило однаково
застосовується до Google, Facebook і GitHub.

Рекомендована модель:

- `provider` — відоме значення на кшталт `GOOGLE`, `APPLE`, `FACEBOOK`, `GITHUB`;
- `provider_subject` — стабільний `sub` або provider user ID;
- `user_id` — посилання на локального User;
- унікальний constraint на пару `provider + provider_subject`.

Після знаходження локального User backend не довіряє ролям від провайдера.
Ролі визначаються правилами VaultNote, а не claims зовнішнього профілю.

## Рішення для VaultNote

На Phase 5 реалізуємо тільки Google:

- один Google OAuth/OIDC client;
- локальний callback через `localhost`;
- scopes `openid email profile`;
- тестування через доданий test user;
- пошук або створення локального User;
- видача наших JWT і refresh cookie.

Apple відкладаємо до моменту, коли буде виправдана платна Apple Developer
інфраструктура. Facebook і GitHub залишаємо кандидатами для наступного
розширення після стабілізації Google flow.

Пов'язана нотатка: [OAuth2/OIDC простими словами](OAuth2-OIDC-простими-словами.md).
