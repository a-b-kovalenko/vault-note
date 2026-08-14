[⬅️](../VaultNote_Atlas.md)

## 📝 TL;DR

**Статус:** `OPEN HARDENING GAP`  
**Тип:** dependency та build supply chain

На момент аудиту не було Gradle dependency locking/verification metadata,
checksum для Gradle wrapper distribution і повного локального SCA/CVE scanner.
Це не підтверджена експлуатація, але послаблює контроль того, що саме потрапляє
у build.

## Що вже відомо

- `npm audit --offline` для frontend і docs показав `0 vulnerabilities`.
- npm lockfiles мають integrity hashes.
- Gradle build використовує `npm ci`.
- Локальних `gitleaks`, `trivy`, `semgrep` або `osv-scanner` не знайдено.
- JVM CVE scan не виконувався через відсутність локального scanner-а.
- Gradle dependency locking/verification metadata відсутні.
- Gradle wrapper URL не має `distributionSha256Sum`.

## У чому ризик

Локальний `npm audit` залежить від кешованої advisory database і не гарантує,
що нові advisory вже відомі. Без Gradle verification складніше виявити
неочікувану або підмінену dependency. Без перевірки wrapper-а build може
завантажити не той distribution, на який розраховувала команда.

Це supply-chain hardening gap, а не доказ, що конкретна dependency вже
скомпрометована.

## Як закрити

1. Увімкнути Gradle dependency locking для конфігурацій, які використовує
   build.
2. Додати dependency verification metadata та review процедуру для змін
   checksum/signature.
3. Додати `distributionSha256Sum` для Gradle wrapper.
4. Додати регулярний SCA scanner у CI та перевірку актуальної advisory database.
5. Залишити `npm ci` і перевіряти зміни lockfiles у code review.
6. Окремо визначити, як сканувати JVM dependencies і container images, коли
   з’явиться production deployment.

## Поточний висновок

Проблему ще не закрито. Потрібен deployment/CI контекст і процес оновлення
залежностей, після чого результат слід повторно перевірити в audit report.
