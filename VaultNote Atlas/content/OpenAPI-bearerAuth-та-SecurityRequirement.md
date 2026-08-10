[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

`OpenApiConfiguration` описує для Swagger/OpenAPI, що API використовує JWT у
форматі HTTP Bearer. `@SecurityRequirement(name = "bearerAuth")` пов'язує цю
схему з конкретним endpoint-ом.

Це документація API, а не сама перевірка доступу. Реальну автентифікацію
виконує Spring Security через `SecurityConfig` і `JwtDecoder`.

## Де це використовується у VaultNote

- **Опис схеми** — `OpenApiConfiguration` реєструє схему з іменем
  `bearerAuth`.
- **Захищений endpoint поточного користувача** — `AuthController` має
  `@SecurityRequirement(name = "bearerAuth")`.
- **Адміністративний список користувачів** — `UserController` має таку саму
  вимогу.
- **Реальний захист запитів** — `SecurityConfig` налаштовує OAuth2 Resource
  Server, який читає Bearer token і передає його в `JwtDecoder`.

## Що робить `OpenApiConfiguration`

Конфігурація створює bean `OpenAPI` із зареєстрованою security scheme:

```java
var bearerAuth = new SecurityScheme()
    .type(SecurityScheme.Type.HTTP)
    .scheme("bearer")
    .bearerFormat("JWT");

return new OpenAPI()
    .components(new Components()
        .addSecuritySchemes("bearerAuth", bearerAuth));
```

Кожен параметр має окрему роль:

- `type(HTTP)` — credential передається через HTTP-заголовок;
- `scheme("bearer")` — токен має передаватися у форматі
  `Authorization: Bearer <token>`;
- `bearerFormat("JWT")` — підказка для документації, що Bearer token має
  формат JWT;
- `addSecuritySchemes("bearerAuth", ...)` — додає схему до каталогу OpenAPI
  під іменем `bearerAuth`.

Ім'я схеми важливе: воно має збігатися з іменем у
`@SecurityRequirement`.

## Що робить `@SecurityRequirement`

На endpoint-і анотація виглядає так:

```java
@SecurityRequirement(name = "bearerAuth")
```

Вона повідомляє OpenAPI, що для виклику операції потрібна схема
`bearerAuth`. У Swagger UI такий endpoint позначається як захищений і може
використовувати токен, введений через кнопку `Authorize`.

Після авторизації Swagger UI додає до запиту:

```http
Authorization: Bearer eyJ...
```

## Що відбувається під час реального запиту

Натискання `Try it out` у Swagger UI лише формує звичайний HTTP-запит. Далі
працює вже Spring Security:

1. `BearerTokenAuthenticationFilter` дістає JWT із заголовка `Authorization`.
2. `JwtAuthenticationProvider` передає token у `JwtDecoder`.
3. `JwtDecoder` перевіряє підпис, issuer і строк дії токена.
4. Spring Security створює authenticated `SecurityContext`.
5. Запит доходить до `AuthController` або `UserController`.
6. `@PreAuthorize` на service-інтерфейсі додатково перевіряє роль, якщо це
   потрібно для конкретного use case.

Якщо токен відсутній або невалідний, запит завершується `401 Unauthorized`. Якщо
токен валідний, але користувач не має потрібної ролі, результатом буде
`403 Forbidden`.

## OpenAPI не замінює Spring Security

Відповідальність розділена між різними компонентами:

- `OpenApiConfiguration` — описує схему для документації;
- `@SecurityRequirement` — позначає захищену операцію в OpenAPI;
- `SecurityConfig` — визначає правила доступу та вмикає JWT authentication;
- `JwtDecoder` — перевіряє справжність і строк дії JWT;
- `@PreAuthorize` — перевіряє роль або інше authorization-правило на service
  boundary.

Якщо прибрати `@SecurityRequirement`, endpoint усе одно може бути захищений
Spring Security, але Swagger UI не матиме повної інформації про вимогу токена.
Якщо прибрати налаштування Spring Security, сама OpenAPI-анотація не захистить
endpoint.

## Чому схема не задана глобально

У VaultNote схема зареєстрована централізовано, але вимога вказується на
конкретних захищених endpoint-ах. Так документація явно показує, які операції
потребують JWT, а які залишаються публічними, наприклад registration або
login.

## Пов'язані рішення

- [JWT-аутентифікація і перевірка доступу](JWT-аутентифікація-і-перевірка-доступу.md)
- [@PreAuthorize на service-інтерфейсі](PreAuthorize-на-service-інтерфейсі.md)
- [OpenAPI, DTO та межі REST](OpenAPI-DTO-та-межі-REST.md)
