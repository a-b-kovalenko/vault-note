[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Angular залишається окремим npm-проєктом у `frontend/`, але Gradle backend
запускає його встановлення залежностей, тести та production build. Так backend
і frontend проходять один quality gate у локальній розробці та CI.

Це частина нотатки [про структуру Angular workspace](Структура-Angular-workspace.md).

## Модель інтеграції

Gradle не замінює npm. Backend Gradle build використовує Node Gradle plugin,
щоб викликати npm scripts у правильній директорії та включити їх у загальну
перевірку репозиторію.

Це схоже на Gradle task, який запускає зовнішній build tool. Сам Angular build
при цьому залишається відповідальністю Angular CLI.

## Доступні Gradle-задачі

- `frontendNpmInstall` встановлює залежності через `npm ci` і
  `package-lock.json`;
- `frontendStart` запускає Angular development server на
  `http://localhost:4200`;
- `frontendTest` запускає unit-тести Angular один раз, без watch-режиму;
- `frontendBuild` створює production bundle у `frontend/dist/frontend`;
- `check` запускає frontend-тести та production build разом із backend і
  Antora-перевірками.

Ті самі frontend-задачі можна запускати окремо з каталогу `backend`:

```shell
./gradlew frontendTest frontendBuild
```

## Запуск frontend

В IntelliJ IDEA frontend можна запускати локальною npm-конфігурацією `VaultNote
Frontend`, яка виконує script `start` із `frontend/package.json`.

Альтернативно Gradle-задача `frontendStart` запускає той самий development
server:

```shell
./gradlew frontendStart
```

Для Gradle-сценарію Node Gradle plugin автоматично завантажує зафіксовану
версію Node.js. Системні `node` та `npm` для нього не потрібні.

## Межа відповідальності

`package.json` описує npm scripts і frontend dependencies, `angular.json`
описує Angular CLI targets, а `build.gradle` лише підключає ці дії до загального
workflow репозиторію.

Зміни у frontend не повинні вимагати дублювання Angular-команд у кількох
місцях. Якщо команда вже є npm script, Gradle має делегувати її npm.
