[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

`@PreAuthorize` краще розміщувати на application service-методі, якщо правило
описує доступ до use case. У нашому випадку перевірка доступу до списку
користувачів має бути частиною `UserService`, а не лише `UserController`.

## Де це використовується у VaultNote

Ця схема призначена для адміністративного endpoint-а:

```http
GET /api/v1/users
Authorization: Bearer <access-token>
```

Пов'язані частини застосунку:

- **Правило доступу** — `UserService` і `@PreAuthorize`.
- **Виконання use case** — `UserServiceImpl`.
- **HTTP-адаптер** — `UserController`.
- **Дані користувачів** — `UserJpaRepository`.
- **Відповідь API** — `UserInfoDto`.
- **Ролі у Spring Security** — `RolesJwtAuthenticationConverter`.

## Чому не тільки controller

Controller відповідає за HTTP-рівень: binding параметрів, пагінацію, DTO та
HTTP-контракт. Якщо `@PreAuthorize` розмістити лише там, захищеним буде тільки
конкретний endpoint.

```text
HTTP request
    ↓
UserController
    ↓
UserService
    ↓
UserJpaRepository
```

Інший controller, scheduled job або message listener може викликати service
напряму й обійти перевірку, яка була тільки в HTTP-адаптері.

Service-рівень є правильною межею, тому що саме він представляє application
операцію `getUsers`, незалежно від способу її виклику.

## Чому можна розмістити анотацію на інтерфейсі

Spring Security підтримує method-security анотації на класах та інтерфейсах.
Для `UserService` це виглядає так:

```java
public interface UserService {

  @PreAuthorize("hasRole('ADMIN')")
  Page<UserInfoDto> getUsers(Pageable pageable);
}
```

Коли `UserService` викликається як Spring bean, method-security proxy перевіряє
authority до фактичного виконання `UserServiceImpl.getUsers(...)`.

У цьому випадку:

- JWT з `ROLE_ADMIN` проходить перевірку;
- JWT з `ROLE_USER` отримує `403 Forbidden`;
- відсутній або невалідний JWT відхиляється раніше з `401 Unauthorized`.

Не потрібно дублювати те саме правило в controller та implementation. Анотація
на інтерфейсі робить authorization частиною service contract.

## `hasRole` і `hasPermission` — не одне й те саме

Для ролі адміністратора потрібно використовувати:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Оскільки `RolesJwtAuthenticationConverter` додає префікс `ROLE_`, Spring шукає
authority `ROLE_ADMIN`.

`hasPermission(...)` призначений для object-level authorization, наприклад
перевірки, чи має користувач доступ до конкретної нотатки. Простий виклик:

```java
@PreAuthorize("hasPermission('ADMIN')")
```

не є перевіркою ролі й без налаштованого `PermissionEvaluator` не підходить для
цього endpoint-а.

## Що залишається відповідальністю controller

Навіть якщо authorization знаходиться в service, controller має правильно
описати HTTP-контракт:

- приймати `Pageable`;
- повертати `Page<UserInfoDto>`;
- не повертати `UserEntity`;
- документувати `200`, `401` і `403`;

`@PreAuthorize` захищає виконання use case, але не замінює DTO mapping,
пагінацію чи перевірки ownership у service та repository.

## Обмеження proxy-підходу

Перевірка спрацьовує, коли метод викликається через Spring-managed bean. Якщо
метод service викликає інший метод того самого об'єкта напряму, self-invocation
обходить proxy, і анотація внутрішнього методу не спрацює.

Тому protected use cases мають викликатися через application bean, а не через
прямі внутрішні виклики між методами одного service.

## Пов'язані рішення

- [JWT-аутентифікація і перевірка доступу](JWT-аутентифікація-і-перевірка-доступу.md)
- [OpenAPI, DTO та межі REST](OpenAPI-DTO-та-межі-REST.md)
