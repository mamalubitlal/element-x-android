# Чатор — Свободный мессенджер

Свободный мессенджер на базе [Matrix](https://matrix.org/) — альтернатива навязанным государством мессенджерам вроде «Макса». Работает, не блокируется, принадлежит вам.

Чатор — это форк [Element X Android](https://github.com/element-hq/element-x-android) с собственной брендинговой идентичностью и русскоязычным интерфейсом. Под капотом — [Matrix Rust SDK](https://github.com/matrix-org/matrix-rust-sdk), UI написан на [Jetpack Compose](https://developer.android.com/jetpack/compose).

## Скачать

[![GitHub Releases](https://img.shields.io/github/v/release/mamalubitlal/element-x-android?label=GitHub%20Releases&logo=github)](https://github.com/mamalubitlal/element-x-android/releases)

APK-файлы доступны на странице [GitHub Releases](https://github.com/mamalubitlal/element-x-android/releases).

## Отличия от Element X

- **Собственный брендинг** — логотип и фирменный стиль Чатора
- **Русский интерфейс** — весь UI на русском языке
- **Групповые звонки через Jitsi** — вместо Element Call используется Jitsi Meet
- **Enterprise-модуль** — включён в сборку

## Скриншоты

Скриншоты скоро появятся.

## Минимальная версия Android

Чатор работает на Android 7.0 (API 24) и выше.

Enterprise-версия требует Android 13 (API 33) и выше.

## Сборка

Клонируйте репозиторий и откройте проект в Android Studio. При сборке выберите конфигурацию `app`.

```bash
git clone https://github.com/mamalubitlal/element-x-android.git
cd element-x-android
```

Подробнее о сборке: [docs/_developer_onboarding.md](docs/_developer_onboarding.md)

## Контрибьют

Хотите помочь проекту? Отлично! Начните с [good first issue](https://github.com/mamalubitlal/element-x-android/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22). Напишите в комментариях к задаче, что берёте её.

Перед началом работы прочитайте [руководство по контрибьюции](CONTRIBUTING.md).

## Поддержка

Нашли баг или столкнулись с крашем? Создайте [GitHub Issue](https://github.com/mamalubitlal/element-x-android/issues) — опишите подробно, что произошло. Отчёты об ошибках можно отправлять из настроек приложения.

## Лицензия

Copyright (c) 2025 Element Creations Ltd.
Copyright (c) 2022 - 2025 New Vector Ltd.

Проект распространяется под лицензией **AGPL-3.0** (GNU Affero General Public License v3).
Подробности — в файле [LICENSE](LICENSE).
