[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

Ця нотатка — коротка карта Angular workspace у `frontend/`. Детальні пояснення
розділені за темами: конфігурація і tooling, Gradle-інтеграція, bootstrap і
routing application та auth feature.

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

Каталог `.angular/` з'являється локально після build і test. Це кеш Angular
CLI, який ігнорується Git і не є частиною workspace.

## Поточний стан

- Angular standalone application має мінімальний application shell.
- Маршрут `/login` завантажує тимчасову login page.
- Auth feature уже містить frontend-моделі та API service.
- Reactive form, повний auth state, access-token memory storage, refresh і
  CSRF behavior будуть додані наступними кроками Phase 4.
- Frontend-тести та production build підключені до backend Gradle quality gate.

## Детальні нотатки

- [Конфігурація та tooling Angular workspace](Angular-workspace-конфігурація-та-tooling.md)
- [Gradle-інтеграція Angular frontend](Angular-frontend-Gradle-інтеграція.md)
- [Bootstrap, routing та application shell](Angular-bootstrap-routing-та-application-shell.md)
- [Providers, browser routing та root shell](Angular-application-providers-routing-root-shell.md)
- [Auth feature Angular frontend](Angular-auth-feature.md)

## Пов'язані нотатки

- [OAuth2/OIDC простими словами](OAuth2-OIDC-простими-словами.md)
- [CSRF та CORS](CSRF-та-CORS.md)
