[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Angular workspace містить не лише компоненти, а й конфігурацію CLI, TypeScript,
npm, тестів і редактора. У `frontend/` зараз є мінімальний standalone-
застосунок із application shell, маршрутом `/login`, auth-моделями, API
сервісом і тимчасовою login page. Reactive form і повний auth state будуть
додані на наступному кроці.

## Загальна структура

```text
frontend/
├── .editorconfig
├── .gitignore
├── .prettierrc
├── .vscode/
├── README.md
├── angular.json
├── package.json
├── package-lock.json
├── public/
├── src/
├── tsconfig.json
├── tsconfig.app.json
└── tsconfig.spec.json
```

Каталог `.angular/` також з'являється локально після build і test, але це кеш
Angular CLI. Він ігнорується Git і не є частиною workspace, який ми передаємо
іншим розробникам або CI.

## Запуск через Gradle

Frontend залишається окремим npm-проєктом, але backend Gradle build запускає
його перевірки через Node Gradle plugin. Це дає один quality gate для всього
репозиторію.

- `frontendNpmInstall` встановлює залежності через `npm ci` і
  `package-lock.json`;
- `frontendStart` запускає Angular development server на `http://localhost:4200`;
- `frontendTest` запускає unit-тести Angular один раз, без watch-режиму;
- `frontendBuild` створює production bundle у `frontend/dist/frontend`;
- `check` запускає frontend-тести та production build разом із backend і
  Antora-перевірками.

Ті самі frontend-задачі можна запускати окремо з каталогу `backend`:

```shell
./gradlew frontendTest frontendBuild
```

У IntelliJ IDEA frontend можна запускати локальною npm-конфігурацією `VaultNote
Frontend`,
яка виконує script `start` із `frontend/package.json`. Альтернативно Gradle
задача `frontendStart` запускає той самий development server.

Для Gradle-сценарію Node Gradle plugin автоматично завантажує зафіксовану
версію Node.js. Системні `node` та `npm` для нього не потрібні.

## Файли кореня workspace

### `.editorconfig`

Спільні правила форматування текстових файлів для IDE та редакторів. Наприклад,
відступи, завершення рядка й кодування файлів.

### `.gitignore`

Список локальних файлів, які не треба комітити. Сюди потрапляють `node_modules`,
Angular cache, build output та інші тимчасові артефакти.

### `.prettierrc`

Налаштування Prettier для форматування TypeScript, HTML, SCSS і JSON. Це
форматування коду, а не перевірка його поведінки.

### `README.md`

Локальна документація Angular workspace: як встановити залежності, запустити
dev server, виконати build і тести. Пізніше тут можна додати frontend-specific
інструкції поверх кореневого README проєкту.

### `angular.json`

Головна конфігурація Angular CLI. Вона описує:

- назву проєкту `frontend`;
- source root `src`;
- entry point `src/main.ts`;
- assets із `public/`;
- глобальні стилі `src/styles.scss`;
- build, serve і test targets;
- production і development configurations.

Це аналог build-конфігурації для Angular. Тут не повинні зберігатися секрети.

### `package.json`

Маніфест npm-проєкту. Містить:

- назву та версію workspace;
- scripts `start`, `build`, `test` і `watch`;
- runtime dependencies Angular і RxJS;
- development dependencies Angular CLI, TypeScript, Vitest і Prettier.

### `package-lock.json`

Зафіксоване дерево точних версій npm-залежностей. Завдяки цьому локальна
машина і CI встановлюють однакові пакети через `npm ci`.

### `tsconfig.json`

Базова конфігурація TypeScript, спільна для application і test compilation.

### `tsconfig.app.json`

Налаштування TypeScript-компіляції production application. Він успадковує
базовий `tsconfig.json` і визначає application entry files.

### `tsconfig.spec.json`

Окрема TypeScript-конфігурація для тестів. Вона додає типи й налаштування,
потрібні Vitest та test files.

## `.vscode/`

Це необов'язкова допоміжна конфігурація для Visual Studio Code:

- `extensions.json` — рекомендовані extensions;
- `launch.json` — шаблони запуску та debugging;
- `tasks.json` — команди, які можна запускати з IDE.

Вона не є частиною runtime Angular application і не впливає на API-контракт.

## `public/`

Статичні файли, які копіюються у build без TypeScript-обробки. Зараз там лише:

- `favicon.ico` — іконка вкладки браузера.

Сюди можна буде додати логотип або інші статичні assets, які не потребують
імпорту з компонента.

## `src/`

Це вихідний код Angular application.

### `src/index.html`

Єдина HTML-сторінка-хост для SPA. Angular підключає compiled JavaScript до
цього документа і монтує application у root element.

Користувацькі екрани не треба розміщувати безпосередньо тут. Вони мають бути
Angular components, які відображаються через routing.

### `src/main.ts`

Entry point application. Він запускає standalone Angular application і передає
їй root component та `ApplicationConfig`.

Це аналог `main` у backend: файл потрібен для старту, але бізнес-логіку сюди
додавати не треба.

### `src/styles.scss`

Глобальні SCSS-стилі application. Тут доречні базові стилі body, загальні
CSS variables і правила, спільні для кількох сторінок.

Стилі конкретного компонента краще тримати поруч із самим компонентом.

### `src/app/app.ts`

Root standalone component. Він є верхнім компонентом application і пізніше
міститиме shell сторінки: наприклад, router outlet або спільну layout-оболонку.

### `src/app/app.html`

HTML-шаблон root component. Він містить лише `<router-outlet />`, тому shell не
змішується з конкретною сторінкою. Login page завантажується через routing.

### `src/app/app.scss`

SCSS-стилі root component. Локальні стилі компонента ізольовані від глобальних
стилів із `src/styles.scss`.

### `src/app/app.config.ts`

Глобальна standalone-конфігурація Angular application. Вона реєструє:

- browser error listeners;
- Angular Router через `provideRouter(routes)`;
- Angular HTTP client через `provideHttpClient()`.

CSRF та auth-specific HTTP behavior буде додано окремими providers/interceptors
на наступних кроках.

### `src/app/app.routes.ts`

Центральний список маршрутів application. Маршрут `/login` завантажує login
feature lazily. Порожній шлях і невідомі шляхи перенаправляються на `/login`.
Protected Notes і profile routes будуть додані пізніше.

### `src/app/app.spec.ts`

Початковий unit-тест root component. Він перевіряє, що application shell
створюється у test environment. Перевірка самої login behavior буде в тестах
auth feature.

Це не integration test backend і не browser e2e test.

### `src/app/auth/`

Перша feature-папка для authentication flow:

- `auth.models.ts` — frontend-моделі login request/response та raw API response;
- `auth-api.service.ts` — HTTP client для `POST /api/v1/auth/login` і мапінг
  backend `snake_case` у frontend `camelCase`;
- `login-page.ts` — тимчасовий routed placeholder. Reactive form і реальний
  login UI будуть додані наступним кроком.

## Що з'явиться пізніше

Під час наступних кроків структура розшириться іншими feature-папками:

```text
src/app/
├── notes/
├── profile/
├── shared/
└── app.routes.ts
```

Їх не створюємо наперед: спочатку додаємо login flow, а потім — потрібні йому
auth service, HTTP interceptors, token state і тести.

## Пов'язані нотатки

- [OAuth2/OIDC простими словами](OAuth2-OIDC-простими-словами.md)
- [CSRF та CORS](CSRF-та-CORS.md)
