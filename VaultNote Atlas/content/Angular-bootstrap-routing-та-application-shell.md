[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Ці файли запускають Angular application, підключають глобальні providers,
описують browser routing і формують root shell. Їхня роль найближча до
bootstrap, application configuration і web routing у Spring, але все працює у
браузері.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

Пояснення самих понять global providers, browser routing і root shell див. у
[концептуальній нотатці про Angular application](Angular-application-providers-routing-root-shell.md).

Наведені аналогії зі Spring/Java допомагають порівняти ролі файлів, але не є
точним відображенням: Angular frontend і Spring backend працюють на різних
рівнях — browser application проти server application.

## `src/index.html`

Єдина HTML-сторінка-хост для SPA. Angular підключає compiled JavaScript до
цього документа і монтує application у root element.

Користувацькі екрани не треба розміщувати безпосередньо тут. Вони мають бути
Angular components, які відображаються через routing.

Це можна порівняти зі статичним `index.html` у Spring Boot, але Angular
використовує його лише як host-документ для SPA. Це не controller і не template
конкретного backend endpoint.

## `src/main.ts`

Entry point application. Він запускає standalone Angular application і передає
їй root component та `ApplicationConfig`.

Це аналог `main` у backend: файл потрібен для старту, але бізнес-логіку сюди
додавати не треба.

Найближча аналогія — клас із `@SpringBootApplication` і виклик
`SpringApplication.run(...)`: `main.ts` запускає Angular application та передає
їй root component і `appConfig`.

## `src/styles.scss`

Глобальні SCSS-стилі application. Тут доречні базові стилі body, загальні CSS
variables і правила, спільні для кількох сторінок.

Стилі конкретного компонента краще тримати поруч із самим компонентом.

## `src/app/app.ts`

Root standalone component. Він є верхнім компонентом application і формує shell
сторінки: наприклад, router outlet або спільну layout-оболонку.

Найближча аналогія — application shell або root layout у UI. Це не аналог
`@RestController`: `app.ts` не обробляє HTTP-запити і не містить backend
business logic.

## `src/app/app.html`

HTML-шаблон root component. Він містить лише `<router-outlet />`, тому shell не
змішується з конкретною сторінкою. Login page завантажується через routing.

Це найближче до server-side layout/template, але в Angular template працює у
браузері, а `<router-outlet />` є місцем для client-side route component.

## `src/app/app.scss`

SCSS-стилі root component. Локальні стилі компонента ізольовані від глобальних
стилів із `src/styles.scss`.

## `src/app/app.config.ts`

Глобальна standalone-конфігурація Angular application. Вона реєструє:

- browser error listeners;
- Angular Router через `provideRouter(routes)`;
- Angular HTTP client через `provideHttpClient()`.

`app.config.ts` — це application wiring: місце, де під час bootstrap
складаються глобальні можливості Angular-застосунку. Найближча аналогія у
Spring — комбінація `@Configuration`, application context і частини
`@Bean`-реєстрацій:

- `providers` схожі на registry beans у Spring Application Context;
- `provideHttpClient()` схожий на реєстрацію `RestClient` або `WebClient`;
- `provideRouter(routes)` відповідає підключенню web-маршрутизації;
- HTTP interceptors подібні до client interceptors або security filters;
- route guards частково нагадують authorization rules.

Це не місце для UI чи бізнес-логіки. Тут пізніше будуть підключені CSRF- та
auth-interceptors, access-token attachment, refresh behavior і route guards.

## `src/app/app.routes.ts`

Центральний список маршрутів application. Маршрут `/login` завантажує login
feature lazily. Порожній шлях і невідомі шляхи перенаправляються на `/login`.
Protected Notes і profile routes будуть додані пізніше.

Найближча аналогія у Spring MVC — `@RequestMapping` і controller routing.
Різниця в тому, що Angular routes перемикають екрани вже в браузері, а не
маршрутизують HTTP-запити на backend controllers.

## `src/app/app.spec.ts`

Початковий unit-тест root component. Він перевіряє, що application shell
створюється у test environment. Перевірка самої login behavior буде в тестах
auth feature.

Це не integration test backend і не browser e2e test.

Найближча аналогія — JUnit smoke test на кшталт перевірки, що Spring
Application Context успішно запускається. Він перевіряє application shell, але
не весь login flow і не backend API.
