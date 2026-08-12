[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Email, який повернув OAuth-провайдер, не повинен бути єдиною підставою для
автоматичного об'єднання акаунтів. Для стабільної зовнішньої identity потрібно
зберігати пару `provider + provider_subject`, а linking нової identity до вже
існуючого VaultNote-акаунта виконувати лише після локальної автентифікації та
явного підтвердження користувача.

## Дві різні ситуації

Потрібно розрізняти:

- **створення локального акаунта** після першого входу через Google;
- **додавання Google як нового способу входу** до вже існуючого акаунта.

У першому випадку локального User ще немає. У другому User уже існує, тому
backend має захистити його від небажаного об'єднання або linking.

## Чому email недостатній

Email — це властивість identity, але не її стабільний ключ. Він може:

- змінитися у провайдера;
- бути прихованим або заміненим relay-адресою;
- мати alias-варіанти;
- по-різному нормалізуватися різними системами;
- збігатися у двох provider identities.

Для Google стабільним ідентифікатором є `sub` із підтвердженої OIDC identity.
Тому зовнішній зв'язок має виглядати так:

```text
provider = GOOGLE
provider_subject = 123456789
user_id = 42
```

**Аналогія:** email схожий на адресу, яку можна змінити, а
`provider + provider_subject` — на номер документа в конкретній системі
ідентифікації.

## Небезпека автоматичного linking

Припустімо, у базі вже є локальний акаунт:

```text
email = andrii@example.com
password_hash = ...
```

Пізніше Google повертає verified email `andrii@example.com`. Якщо backend
автоматично прив'яже Google identity до знайденого User лише через email, то
OAuth-вхід одразу отримає доступ до існуючого password-акаунта.

Це може бути небезпечно через:

- помилки в нормалізації email;
- особливості alias-адрес;
- різні гарантії верифікації email у провайдерів;
- зміну email або втрату доступу до provider account;
- небажане об'єднання двох акаунтів.

Навіть `email_verified = true` підтверджує контроль над email у провайдера,
але саме по собі не доводить, що користувач хоче об'єднати цей Google-акаунт із
конкретним VaultNote User.

## Рішення для VaultNote

У VaultNote використовуємо такий порядок:

1. Спочатку шукаємо identity за парою `provider + provider_subject`.
2. Якщо identity знайдена — входимо у пов'язаний User.
3. Якщо identity нова, але email не знайдений — запускаємо onboarding.
4. Якщо identity нова, але такий email уже є у VaultNote — не виконуємо
   автоматичний linking.
5. Для linking користувач спочатку входить у локальний акаунт, після чого явно
   підтверджує додавання Google.

## Onboarding для нового Google-користувача

Якщо Google identity ще не пов'язана і локального User із таким email немає,
показуємо окремий onboarding:

- verified email із Google показуємо як read-only;
- `displayName` отримуємо з профілю або просимо підтвердити/змінити;
- створюємо локального User;
- встановлюємо `email_verified = true`;
- призначаємо роль `USER`;
- залишаємо `password_hash = null`, якщо локальний пароль ще не заданий;
- зберігаємо provider identity;
- видаємо звичайні VaultNote access JWT і refresh cookie.

Телефон для цього flow не потрібен: VaultNote не має бізнес-вимоги до SMS
автентифікації.

## Явний linking для існуючого User

Приклад безпечного flow:

1. Користувач входить у VaultNote локальним email/password способом.
2. У налаштуваннях обирає «Додати Google».
3. Backend запускає OAuth flow із новим `state`.
4. Google підтверджує identity.
5. Backend перевіряє `provider_subject`, `state` та відсутність дубліката.
6. Користувач явно підтверджує linking.
7. Backend зберігає зв'язок із поточним User.

У базі не повинно бути двох VaultNote User для однієї пари
`provider + provider_subject`. Для цього потрібне унікальне обмеження.

## Що не повинен робити backend

- не використовувати email як єдиний зовнішній ідентифікатор;
- не прив'язувати Google identity мовчки лише через збіг email;
- не приймати ролі з Google як ролі VaultNote;
- не передавати provider access token або VaultNote JWT у redirect URL;
- не дозволяти користувачу прив'язати identity, яка вже належить іншому User.

Провайдер підтверджує identity, але правила акаунтів, ролей і linking належать
VaultNote.

## Зв'язок із password management

Google-користувач може спочатку мати тільки зовнішній спосіб входу й
`password_hash = null`. Тому перед реалізацією OAuth у VaultNote потрібні:

- nullable `users.password_hash`;
- authenticated set/change-password endpoint;
- заборона видалити останній доступний спосіб автентифікації.

Після set-password один User може входити і через Google, і через локальний
пароль без створення другого акаунта.

Пов'язані нотатки:

- [OAuth2/OIDC простими словами](OAuth2-OIDC-простими-словами.md)
- [OAuth-провайдери: Google, Apple, Facebook і GitHub](OAuth-провайдери-Google-Apple-Facebook.md)
- [Паролі: validation та Argon2id](Паролі-validation-та-Argon2id.md)
