# Windows SmartScreen и подпись издателя / Windows security

## Русский

На данный момент у WristPorter **нет доверенной цифровой подписи Authenticode**. Microsoft Defender SmartScreen может показать красное предупреждение **«Система Windows защитила ваш компьютер»** и издателя «Неизвестный издатель».

SmartScreen оценивает репутацию самого файла и его издателя. После изменения EXE может потребоваться накопление репутации заново, поэтому предыдущая версия могла не вызывать такого предупреждения. **Мы не отключаем и не обходим защиту Windows программно.**

Скачивайте только из [официального GitHub Releases](https://github.com/aidarnig/WristPorter/releases). Сравнивайте SHA-256 ZIP с файлом контрольной суммы. Это проверяет совпадение опубликованного файла, но не подтверждает добросовестность издателя и не является антивирусной экспертизой.

Для устранения проблемы в будущих релизах потребуется цифровая подпись доверенным сертификатом, хранение ключа вне репозитория, проверка подписи в CI и репутация издателя. **Самоподписанный сертификат не решает эту проблему.** Даже доверенная подпись не гарантирует немедленного исчезновения SmartScreen.

## English

WristPorter is currently **unsigned by a trusted Authenticode publisher**. Microsoft Defender SmartScreen may display an **Unknown publisher** warning on a new EXE, even if an older version did not.

We do not bypass Windows security controls. Download only from [official GitHub Releases](https://github.com/aidarnig/WristPorter/releases) and verify the ZIP SHA-256 against the published checksum. A checksum verifies byte integrity relative to the release, not publisher trust or absence of malware.

Future releases should use a trusted signing identity with protected credentials, verified signing in CI, and time to build publisher reputation. Self-signed certificates are not sufficient; a valid signature alone does not guarantee zero warnings.

Microsoft documentation: [SmartScreen reputation](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation).
