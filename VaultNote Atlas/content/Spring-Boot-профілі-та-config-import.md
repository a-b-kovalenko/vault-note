[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Spring Boot автоматично завантажує базовий `application.yaml` і конфігурацію
активного профілю, наприклад `application-local.yaml`. Довільні файли на
кшталт `application-local-secrets.yaml` автоматично не підтягуються: їх треба
явно додати через `spring.config.import`.

## Які файли Spring Boot завантажує автоматично

За стандартною схемою Spring Boot шукає конфігурацію з іменами:

| Файл | Коли завантажується |
| --- | --- |
| `application.yaml` | Завжди як базова конфігурація. |
| `application-local.yaml` | Коли активний profile `local`. |
| `application-test.yaml` | Коли активний profile `test`. |

Profile можна активувати через environment variable:

```shell
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

А в IntelliJ IDEA достатньо вказати `local` у Spring Boot run configuration.

Назва `application-local-secrets.yaml` сама по собі не означає, що файл буде
завантажений для profile `local`. Це окремий довільний файл.

## Явний імпорт додаткового файла

У VaultNote `application-local.yaml` містить:

```yaml
spring:
  config:
    import: optional:classpath:application-local-secrets.yaml
```

Це означає:

1. Spring активує profile `local`.
2. Завантажує `application-local.yaml`.
3. Обробляє `spring.config.import`.
4. Шукає `application-local-secrets.yaml` у classpath.
5. Додає його властивості до Spring `Environment`.

Файл містить звичайну конфігураційну властивість:

```yaml
app:
  jwt:
    secret: <local-secret>
```

Після цього `app.jwt.secret` прив'язується до поля `secret` у
`JwtProperties`, а `JwtConfiguration` перевіряє його довжину та заборонені
placeholder-значення.

## Що означає `classpath`

Файл лежить у `backend/src/main/resources`. Під час Gradle-запуску resources
копіюються до `build/resources/main`, а IntelliJ додає їх до classpath модуля.
Тому одна й та сама конфігурація працює через `bootRun` і через IntelliJ.

`.gitignore` не впливає на Spring Boot. Він лише не дозволяє Git побачити
локальний secrets-файл; якщо файл фізично існує, Spring може його прочитати.

## Навіщо потрібен `optional`

`optional:classpath:...` означає, що відсутній файл не спричинить окрему
помилку імпорту. Це зручно для чистого checkout, де локальний secrets-файл
ще не створено.

Водночас `optional` не створює безпечне значення за замовчуванням. Якщо secret
не знайдено ні в локальному файлі, ні в environment, застосунок завершує
startup з помилкою конфігурації.

## Локальний secret і deployment secret

Локальний secret зберігається в ignored-файлі:

```text
backend/src/main/resources/application-local-secrets.yaml
```

Для CI, контейнерів або інших середовищ використовується
`VAULTNOTE_JWT_SECRET`. Відомий fallback у tracked-конфігурації не потрібен:
локальна конфігурація має стабільний secret між перезапусками, а production
отримує його з environment або secret manager.

Пов'язана нотатка: [JWT-аутентифікація і перевірка доступу](JWT-аутентифікація-і-перевірка-доступу.md).
