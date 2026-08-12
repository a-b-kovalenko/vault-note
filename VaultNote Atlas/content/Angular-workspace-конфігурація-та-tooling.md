[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Конфігураційні файли Angular workspace визначають правила форматування,
залежності, TypeScript-компіляцію, CLI targets і локальні налаштування IDE.
Вони описують інструменти та build-процес, а не runtime business logic.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

## Файли кореня workspace

### `.editorconfig`

Спільні правила форматування текстових файлів для IDE та редакторів: відступи,
завершення рядка й кодування файлів.

### `.gitignore`

Список локальних файлів, які не треба комітити. Сюди потрапляють `node_modules`,
Angular cache, build output та інші тимчасові артефакти.

### `.prettierrc`

Налаштування Prettier для форматування TypeScript, HTML, SCSS і JSON. Це
форматування коду, а не перевірка його поведінки.

Найближча аналогія у Java-проєкті — конфігурація Spotless або іншого formatter.
Як і Spotless, `.prettierrc` впливає на вигляд коду, але не реєструє runtime
компоненти.

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

Найближча аналогія у Spring-проєкті — `build.gradle` або `pom.xml`: `angular.json`
описує targets для build, serve і test, але не налаштовує Spring Application
Context.

### `package.json`

Маніфест npm-проєкту. Містить:

- назву та версію workspace;
- scripts `start`, `build`, `test` і `watch`;
- runtime dependencies Angular і RxJS;
- development dependencies Angular CLI, TypeScript, Vitest і Prettier.

Найближча аналогія — `build.gradle` або `pom.xml` для frontend: тут описані
залежності та команди, якими керуємо npm-проєктом.

### `package-lock.json`

Зафіксоване дерево точних версій npm-залежностей. Завдяки цьому локальна
машина і CI встановлюють однакові пакети через `npm ci`.

Найближча аналогія — Gradle dependency lockfile. Обидва файли фіксують точне
дерево залежностей, хоча `package-lock.json` належить npm-екосистемі.

### `tsconfig.json`

Базова конфігурація TypeScript, спільна для application і test compilation.

У Java найближча аналогія — налаштування компілятора та toolchain у Gradle:
версія мови, compiler options і спільні правила компіляції.

### `tsconfig.app.json`

Налаштування TypeScript-компіляції production application. Він успадковує
базовий `tsconfig.json` і визначає application entry files.

Це подібно до конфігурації main source set у Gradle: вона визначає, як
компілюється production-код application.

### `tsconfig.spec.json`

Окрема TypeScript-конфігурація для тестів. Вона додає типи й налаштування,
потрібні Vitest та test files.

Це подібно до окремого test source set і test classpath у Gradle. Файл не
налаштовує runtime application, а лише середовище компіляції та запуску тестів.

## `.vscode/`

Необов'язкова допоміжна конфігурація для Visual Studio Code:

- `extensions.json` — рекомендовані extensions;
- `launch.json` — шаблони запуску та debugging;
- `tasks.json` — команди, які можна запускати з IDE.

Вона не є частиною runtime Angular application і не впливає на API-контракт.

Найближча аналогія — `.idea` або IntelliJ run configuration. Це локальна
конфігурація інструментів розробника, а не Spring-конфігурація.

## `public/`

Статичні файли, які копіюються у build без TypeScript-обробки. Зараз там лише:

- `favicon.ico` — іконка вкладки браузера.

Сюди можна буде додати логотип або інші статичні assets, які не потребують
імпорту з компонента.

Найближча аналогія у Spring Boot — `src/main/resources/static`: обидва каталоги
містять статичні ресурси. В Angular вони копіюються у frontend bundle під час
build.
