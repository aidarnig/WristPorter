# WristPorter — передача фотографий на Wear OS

[![Версия](https://img.shields.io/github/v/release/aidarnig/WristPorter?label=Windows%20portable)](https://github.com/aidarnig/WristPorter/releases/latest) [![Сборка](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml/badge.svg)](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml) [![MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[English](README.md) · [**Русский**](README.ru.md)

![WristPorter — логотип](docs/assets/social-preview.png)

**Передавайте JPG и PNG на часы Wear OS с Windows через беспроводной ADB — без Android-смартфона после первоначальной настройки часов.** WristPorter — бесплатное приложение с открытым исходным кодом для Windows и совместимых часов Wear OS. Основная проверенная модель — Samsung Galaxy Watch 4 Classic.

## Скачать

**[Скачать актуальную portable-версию из GitHub Releases](https://github.com/aidarnig/WristPorter/releases/latest)**

В разделе **Assets** выберите `WristPorter-1.1.2-windows-x64.zip`, а не автоматически созданный GitHub архив **Source code**. Полностью распакуйте ZIP и запустите `WristPorter.exe` из папки `WristPorter`. Java, JavaFX и ADB со всеми необходимыми DLL уже входят в комплект. Не нужны установщик, Android Studio или приложение на смартфоне после первоначальной настройки часов.

## Возможности

- Сопряжение и подключение часов по Wi-Fi через ADB, обнаружение устройств после перезапуска.
- Выбор JPEG/PNG, целых папок и перетаскивание файлов.
- Просмотр фотографий в адаптивной сетке миниатюр.
- Отправка пачки изображений с крупным прогресс-баром и результатом по каждому файлу; автоматическое обновление Samsung Gallery.
- Повтор неудачных операций без лишнего копирования уже загруженных изображений.
- Переключение **Русский / English** и **светлой / тёмной темы** без перезапуска с сохранением настроек.

## Реальный скриншот

![WristPorter на русском — успешно переданы девять фотографий](docs/assets/ui-screenshot-ru.webp)

*WristPorter 1.1.2 в Windows, русский интерфейс: **9 из 9 фотографий отправлено, 0 ошибок**. Настоящий скриншот, адрес часов скрыт.*

## Как это работает

1. Включите на часах параметры разработчика и **Беспроводную отладку**.
2. Выполните сопряжение через **порт сопряжения** и шестизначный код.
3. Подключитесь через **другой порт отладки**, отображаемый на часах.
4. Добавьте JPG/PNG в очередь и нажмите **Передать на часы**.
5. Откройте Samsung Gallery — программа автоматически запрашивает индексацию скопированных изображений.

Подробная инструкция — [Быстрый старт](docs/QUICK_START.ru.md).

После передачи фотографий рекомендуется отключить **Беспроводную отладку** на часах.

> **SmartScreen:** текущий EXE не имеет доверенной подписи издателя. Сверяйте источник и SHA-256. [Подробнее](docs/SECURITY.md).

## Для разработчиков

Для сборки из исходников потребуются JDK 25 и Maven 3.9+:

```bash
mvn -B -ntp verify
mvn -B -ntp -pl watch-desktop -am install
mvn -pl watch-desktop javafx:run
```

Технические документы: [Архитектура](docs/ARCHITECTURE.md) · [UX](docs/UX.md) · [Тестирование](docs/QUALITY.md) · [Разработка](docs/DEVELOPMENT.md) · [CI](docs/CI.md) · [План](docs/ROADMAP.md) · [Брендинг](docs/BRANDING.md) · [Задачи](https://github.com/aidarnig/WristPorter/issues).

Лицензия MIT. Проект независимый и не связан официально с Samsung или Google. Основной сценарий проверен на Galaxy Watch 4 Classic; работа на других часах и в нестандартных сетях не гарантируется.
