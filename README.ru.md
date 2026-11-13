# WristPorter — фотографии с Windows на часы Wear OS

[![Версия для Windows](https://img.shields.io/github/v/release/aidarnig/WristPorter?label=Windows%20download)](https://github.com/aidarnig/WristPorter/releases/latest) [![Сборка](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml/badge.svg)](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml) [![Лицензия MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[English](README.md) · [**Русский**](README.ru.md)

![WristPorter](docs/assets/social-preview.png)

**Передавайте фотографии JPG и PNG с компьютера Windows прямо на часы Wear OS по Wi-Fi.** На проверенных Galaxy Watch4 Classic фотографии появляются в Samsung Gallery. Для передачи не нужны кабель, облачный аккаунт или приложение на телефоне.

**[Скачать WristPorter для Windows](https://github.com/aidarnig/WristPorter/releases/latest)** · [Быстрый старт](docs/QUICK_START.ru.md) · [Совместимые часы](docs/COMPATIBILITY.ru.md)

WristPorter — бесплатная программа с открытым исходным кодом. В portable-архив уже включены Java и ADB: устанавливать Android Studio и Java отдельно не нужно.

## Скриншоты

![Интерфейс WristPorter в Windows](docs/assets/ui-screenshot-ru.webp)

## Почему я создал WristPorter

Когда я перешёл со смартфона Samsung на iPhone, то лишился привычного способа отправлять фотографии на Galaxy Watch. Но мне по-прежнему хотелось хранить снимки в галерее часов. Так появился WristPorter: выбираю фотографии на компьютере Windows, подключаю часы по Wi-Fi и отправляю их напрямую.

**Перешли на iPhone?** WristPorter поможет передавать фото на *уже настроенные* совместимые часы, даже если Android-телефона больше нет. Программа **не подключает Galaxy Watch к iPhone** и не заменяет Android-смартфон, необходимый для первоначальной настройки многих моделей Galaxy Watch.

## Как это работает

1. На часах включите **Параметры разработчика → Отладка по ADB → Беспроводная отладка**.
2. Выполните сопряжение и подключение в WristPorter. **Порт сопряжения отличается от порта подключения**.
3. Добавьте JPG/PNG или папку с фотографиями (можно перетащить файлы в окно) и нажмите **Передать на часы**.
4. Откройте Галерею на часах. После копирования WristPorter запрашивает индексацию изображений; их появление зависит от прошивки и галереи конкретных часов.

[Пошаговая инструкция по подключению](docs/QUICK_START.ru.md).

## Совместимость с часами

WristPorter использует стандартный беспроводной ADB, а не закрытый способ передачи Samsung. Поэтому программа **потенциально** может работать и с другими Wear OS-часами с беспроводной отладкой. Однако полный сценарий от передачи до появления фото в галерее проверен только на одной модели.

| Часы | Статус |
| --- | --- |
| **Samsung Galaxy Watch4 Classic** | **Проверено:** сопряжение, передача и появление фото в Samsung Gallery |
| Другие Samsung Galaxy Watch на Wear OS | Потенциально совместимы — не проверены |
| Google Pixel Watch, OnePlus Watch, Xiaomi Watch, TicWatch, OPPO Watch X и другие линейки Wear OS | Потенциально совместимы — не проверены |

Успешное подключение по ADB **не гарантирует** отображение фото в галерее на любых часах. Старые Samsung на Tizen, Apple Watch и часы на других ОС не поддерживаются.

Подробнее — [список моделей и требования](docs/COMPATIBILITY.ru.md). **Проверили WristPorter на других часах?** [Расскажите о результате](https://github.com/aidarnig/WristPorter/issues/new?template=watch-compatibility.yml).

## Возможности

- Отправка нескольких JPG/PNG сразу и импорт целых папок.
- Просмотр миниатюр до отправки.
- Сопряжение и поиск часов прямо в программе, без командной строки.
- Прогресс передачи, отмена и повтор неудачных операций.
- Русский и английский языки, светлая и тёмная темы.

## Скачивание и запуск

1. Откройте [последний релиз](https://github.com/aidarnig/WristPorter/releases/latest) и скачайте **WristPorter-…-windows-x64.zip** в разделе **Assets** (не архив *Source code*).
2. Распакуйте **весь архив**, откройте папку WristPorter и запустите **WristPorter.exe**.
3. Подключите часы по [инструкции](docs/QUICK_START.ru.md).

**Требования:** Windows x64, ранее настроенные часы Wear OS с беспроводной отладкой и сеть Wi-Fi, в которой компьютер может обращаться к часам. Работа проверена на Galaxy Watch4 Classic; остальные модели и чистые установки Windows пока не прошли полную проверку.

Программа работает локально, без аккаунтов и сбора телеметрии. После передачи **отключайте беспроводную отладку** на часах.

**Windows SmartScreen:** EXE пока не подписан сертификатом доверенного издателя. Скачивайте программу только из этого репозитория, сравнивайте SHA-256 с опубликованной контрольной суммой и прочитайте [информацию о безопасности](docs/SECURITY.md). Не отключайте защиту Windows.

## Разработка и обратная связь

- [Сборка из исходников](docs/DEVELOPMENT.md) · [Архитектура](docs/ARCHITECTURE.md) · [Тестирование](docs/QUALITY.md)
- [Как помочь проекту](CONTRIBUTING.ru.md) · [Задачи и ошибки](https://github.com/aidarnig/WristPorter/issues) · [Сообщить о совместимости](https://github.com/aidarnig/WristPorter/issues/new?template=watch-compatibility.yml)

Лицензия [MIT](LICENSE). Проект независимый и не связан официально с Samsung, Google или производителями часов.
